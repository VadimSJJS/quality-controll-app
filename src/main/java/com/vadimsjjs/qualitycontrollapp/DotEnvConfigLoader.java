package com.vadimsjjs.qualitycontrollapp;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DotEnvConfigLoader {

    private static final String ENV_FILE = ".env";
    private static final String ENV_LOCAL_FILE = ".env.local";

    /** Строка вида KEY=value или export KEY=value (ключ только верхнего регистра, как принято в .env). */
    private static final Pattern ENV_LINE =
            Pattern.compile("^\\s*(?:export\\s+)?([A-Za-z_][A-Za-z0-9_]*)\\s*=");

    public static void loadDotEnv(ConfigurableEnvironment environment) {
        Properties base = readEnvFile(ENV_FILE);
        Properties local = readEnvFile(ENV_LOCAL_FILE);

        if (!base.isEmpty()) {
            environment.getPropertySources()
                    .addFirst(new PropertiesPropertySource("dotenv", base));
        }
        if (!local.isEmpty()) {
            environment.getPropertySources()
                    .addFirst(new PropertiesPropertySource("dotenv-local", local));
            System.out.println("[ENV] " + ENV_LOCAL_FILE + " перекрывает значения из " + ENV_FILE);
        }

        dropEmptyValuesFromOtherDotenvSources(environment);
        applyEffectiveSslSettings(environment);
        logSslConfiguration(environment);
    }

    /**
     * Читает файл .env / .env.local из рабочего каталога приложения.
     * Пустые значения (KEY=) игнорируются, чтобы не перекрывать значения по умолчанию.
     * Берутся только ключи, реально записанные в файле (библиотека dotenv также
     * подмешивает переменные окружения ОС — они здесь не нужны).
     */
    private static Properties readEnvFile(String fileName) {
        Properties props = new Properties();
        Path path = Path.of(fileName);
        if (!Files.isRegularFile(path)) {
            return props;
        }

        Set<String> fileKeys = new HashSet<>();
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                Matcher matcher = ENV_LINE.matcher(line);
                if (matcher.find()) {
                    fileKeys.add(matcher.group(1));
                }
            }
        } catch (Exception e) {
            System.err.println("Не удалось прочитать " + fileName + ": " + e.getMessage());
            return props;
        }

        try {
            Dotenv dotenv = Dotenv.configure()
                    .filename(fileName)
                    .ignoreIfMissing()
                    .ignoreIfMalformed()
                    .load();

            dotenv.entries().forEach(entry -> {
                if (!fileKeys.contains(entry.getKey())) {
                    return;
                }
                String value = entry.getValue();
                if (value == null || value.isEmpty()) {
                    System.out.println("[ENV] " + fileName + ": " + entry.getKey()
                            + " — пустое значение, игнорирую (будет взято значение по умолчанию)");
                    return;
                }
                props.setProperty(entry.getKey(), value);
            });

            if (!props.isEmpty()) {
                System.out.println("[ENV] Загружен " + fileName + " (" + props.size() + " значений)");
            }
        } catch (Exception e) {
            System.err.println("Не удалось загрузить " + fileName + ": " + e.getMessage());
        }
        return props;
    }

    /**
     * Вычисляет эффективные настройки HTTPS из значений .env и записывает их в первый
     * по приоритету источник свойств. Нужно потому, что пустые значения (SSL_KEY_PASSWORD=)
     * из .env попадают в другие источники и перекрывают значения по умолчанию из
     * application.properties — тогда Tomcat падает с ошибкой
     * "Get Key failed: Given final block not properly padded".
     * Пустое значение здесь считается незаданным.
     */
    private static void applyEffectiveSslSettings(ConfigurableEnvironment environment) {
        String store = firstNonBlank(environment, "SSL_STORE", "SERVER_SSL_KEY_STORE");
        String storeType = firstNonBlank(environment, "SSL_STORE_TYPE");
        String storePassword = firstNonBlank(environment, "SSL_PASSWORD", "SSL_PASS",
                "SERVER_SSL_KEY_STORE_PASSWORD");
        String keyPassword = firstNonBlank(environment, "SSL_KEY_PASSWORD", "SSL_PASS");
        String alias = firstNonBlank(environment, "SSL_KEY_ALIAS", "SSL_ALIAS", "SERVER_SSL_KEY_ALIAS");
        String enabled = firstNonBlank(environment, "SSL_ENABLED");

        Properties effective = new Properties();
        if (store != null) {
            effective.setProperty("server.ssl.key-store", store);
        }
        if (storeType != null) {
            effective.setProperty("server.ssl.key-store-type", storeType);
        }
        if (storePassword != null) {
            effective.setProperty("server.ssl.key-store-password", storePassword);
            // Для PKCS12 пароль ключа совпадает с паролем кейстора.
            effective.setProperty("server.ssl.key-password",
                    keyPassword != null ? keyPassword : storePassword);
        } else if (keyPassword != null) {
            effective.setProperty("server.ssl.key-password", keyPassword);
        }
        if (alias != null) {
            effective.setProperty("server.ssl.key-alias", alias);
        }
        if (enabled != null) {
            effective.setProperty("server.ssl.enabled", enabled);
        }

        if (!effective.isEmpty()) {
            environment.getPropertySources()
                    .addFirst(new PropertiesPropertySource("dotenv-ssl", effective));
        }
    }

    private static String firstNonBlank(ConfigurableEnvironment environment, String... keys) {
        for (String key : keys) {
            String value = environment.getProperty(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * Некоторые dotenv-библиотеки регистрируют собственный источник свойств "env" из того же .env,
     * и пустые значения (например SSL_KEY_PASSWORD=) остаются в нём как пустая строка. Из-за этого
     * плейсхолдер ${SSL_KEY_PASSWORD:...} не переходит к значению по умолчанию, и приложение
     * падает с ошибкой "Get Key failed: Given final block not properly padded".
     * Здесь такие записи удаляются — пустое значение считается незаданным.
     */
    private static void dropEmptyValuesFromOtherDotenvSources(ConfigurableEnvironment environment) {
        java.util.List<PropertySource<?>> sourcesToFix = new java.util.ArrayList<>();
        for (PropertySource<?> source : environment.getPropertySources()) {
            boolean dotenvLike = "env".equals(source.getName())
                    || source.getClass().getSimpleName().contains("Dotenv");
            if (dotenvLike && source instanceof EnumerablePropertySource) {
                sourcesToFix.add(source);
            }
        }

        for (PropertySource<?> source : sourcesToFix) {
            EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
            java.util.Map<String, Object> filtered = new java.util.LinkedHashMap<>();
            boolean removedAny = false;
            for (String key : enumerable.getPropertyNames()) {
                Object value = enumerable.getProperty(key);
                if (value == null || value.toString().isEmpty()) {
                    removedAny = true;
                    continue;
                }
                filtered.put(key, value);
            }
            if (removedAny) {
                environment.getPropertySources()
                        .replace(source.getName(), new MapPropertySource(source.getName(), filtered));
                System.out.println("[ENV] в источнике '" + source.getName()
                        + "' проигнорированы пустые значения (будут взяты значения по умолчанию)");
            }
        }
    }

    /**
     * Печатает итоговую конфигурацию HTTPS (после .env и application.properties).
     * Выполняется до старта Tomcat, поэтому при ошибке вида
     * "Get Key failed: Given final block not properly padded" сразу видно,
     * какой кейстор, пароль и alias фактически использует приложение.
     */
    private static void logSslConfiguration(ConfigurableEnvironment environment) {
        String enabled = environment.getProperty("server.ssl.enabled");
        String storeType = environment.getProperty("server.ssl.key-store-type");
        String store = environment.getProperty("server.ssl.key-store");
        String alias = environment.getProperty("server.ssl.key-alias");
        String storePassword = environment.getProperty("server.ssl.key-store-password");
        String keyPassword = environment.getProperty("server.ssl.key-password");

        System.out.println("========== SSL-конфигурация приложения ==========");
        System.out.println("  enabled      : " + enabled);
        System.out.println("  store type   : " + storeType);
        System.out.println("  key-store    : " + store);
        System.out.println("  key-alias    : " + alias);
        System.out.println("  store-passwd : " + describe(storePassword));
        System.out.println("  key-passwd   : " + describe(keyPassword)
                + (isSame(storePassword, keyPassword) ? "  (совпадает со store-passwd)" : "  (ОТЛИЧАЕТСЯ от store-passwd!)"));

        if (store != null && store.startsWith("file:")) {
            File file = new File(store.substring("file:".length()));
            System.out.println("  рабочий каталог: " + Path.of("").toAbsolutePath());
            System.out.println("  файл кейстора  : " + file.getAbsolutePath()
                    + (file.isFile()
                        ? "  [найден, " + file.length() + " байт]"
                        : "  [НЕ НАЙДЕН — будет ошибка старта!]"));
        } else {
            System.out.println("  кейстор берётся из classpath (встроен в jar)");
        }
        System.out.println("=================================================");
    }

    private static boolean isSame(String first, String second) {
        return first != null && first.equals(second);
    }

    private static String describe(String password) {
        if (password == null) {
            return "не задан";
        }
        return "*** (" + password.length() + " симв.)";
    }
}