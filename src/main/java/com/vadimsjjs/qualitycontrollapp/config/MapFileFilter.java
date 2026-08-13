package com.vadimsjjs.qualitycontrollapp.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class MapFileFilter implements Filter, Ordered {
    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        if (uri.endsWith(".map")) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(404);
            return;
        }
        chain.doFilter(request, response);
    }
}