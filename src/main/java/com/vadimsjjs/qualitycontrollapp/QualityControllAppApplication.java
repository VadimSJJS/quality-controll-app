package com.vadimsjjs.qualitycontrollapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Точка входа приложения.
 *
 * <p>Здесь подключается загрузка переменных из .env и .env.local — она должна выполняться
 * до создания Spring-контекста, иначе значения из файлов не попадут в настройки.
 */
@SpringBootApplication
public class QualityControllAppApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(QualityControllAppApplication.class);
        app.addInitializers(context -> {
            DotEnvConfigLoader.loadDotEnv((ConfigurableEnvironment) context.getEnvironment());
        });
        app.run(args);
    }
}
