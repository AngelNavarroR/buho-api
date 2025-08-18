package org.angbyte.config;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    @Autowired
    private BuhoProperties buhoProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (request.getContentType() != null && request.getContentType().contains("application/json")) {
            String requestBody = new String(request.getInputStream().readAllBytes());
            String message = (requestBody == null || requestBody.isEmpty()) ? "No hay body" : requestBody;
            if (message.length() > 500) {
                message = message.substring(0, 500) + "...";
            }
            if (buhoProperties.isDebug())
                logger.debug("[ANTES DE PROCESAR] Request Body: {}", message);

            request = new CustomHttpServletRequestWrapper(request, requestBody);
        }

        filterChain.doFilter(request, response);
    }

    private static class CustomHttpServletRequestWrapper extends HttpServletRequestWrapper {
        private final String body;

        public CustomHttpServletRequestWrapper(HttpServletRequest request, String body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }

                @Override
                public void setReadListener(ReadListener readListener) {
//                    if (readListener == null) {
//                        throw new IllegalArgumentException("ReadListener no puede ser nulo");
//                    }

                    try {
                        if (!isFinished()) {
                            readListener.onDataAvailable();
                        } else {
                            readListener.onAllDataRead();
                        }
                    } catch (IOException e) {
                        readListener.onError(e);
                    }
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(this.getInputStream()));
        }
    }
}