package com.vadimsjjs.qualitycontrollapp.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.CharacterEncodingFilter;

/**
 * Принудительная кодировка UTF-8 для запросов, ответов и JSON.
 * Без этого русские тексты в отчётах и сообщения выводятся некорректно.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebConfig {

    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        return filter;
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatFactoryCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> connector.setURIEncoding("UTF-8"));
    }
}
