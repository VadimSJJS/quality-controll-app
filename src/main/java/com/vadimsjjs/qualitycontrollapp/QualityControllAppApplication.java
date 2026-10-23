package com.vadimsjjs.qualitycontrollapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

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
