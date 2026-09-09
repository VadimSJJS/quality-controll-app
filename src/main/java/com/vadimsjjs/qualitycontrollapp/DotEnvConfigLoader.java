package com.vadimsjjs.qualitycontrollapp;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

public class DotEnvConfigLoader {

    public static void loadDotEnv(ConfigurableEnvironment environment) {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .filename(".env")
                    .ignoreIfMissing()
                    .ignoreIfMalformed()
                    .load();

            Properties props = new Properties();
            dotenv.entries().forEach(entry ->
                    props.setProperty(entry.getKey(), entry.getValue())
            );

            environment.getPropertySources()
                    .addFirst(new PropertiesPropertySource("dotenv", props));
        } catch (Exception e) {
            System.err.println("Не удалось загрузить .env: " + e.getMessage());
        }
    }
}