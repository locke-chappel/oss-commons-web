package io.github.lc.oss.commons.web.filters;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SecurityHeadersFilter extends OncePerRequestFilter {
    private Map<String, String> defaultHeaders;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        this.getHeaders().entrySet().stream().forEach(e -> {
            Collection<String> existing = response.getHeaders(e.getKey());
            if (existing == null || existing.isEmpty()) {
                response.addHeader(e.getKey(), e.getValue());
            }
        });
        filterChain.doFilter(request, response);
    }

    protected String getCors() {
        return "";
    }

    protected Map<String, String> getHeaders() {
        if (this.defaultHeaders == null) {
            String cors = this.getCors();
            cors = cors == null ? "" : cors.trim();
            if (!cors.equals("")) {
                cors = " " + cors.trim();
            }

            Map<String, String> map = new HashMap<>();

            /* OWASP - Content Security Policy */
            map.put("Content-Security-Policy", "default-src 'none'; script-src 'self'; " + //
                    "connect-src 'self'" + cors + "; img-src 'self'; style-src 'self'; " + //
                    "font-src 'self'; frame-ancestors 'none';");

            /* OWASP Prevent browsers from guessing at media types */
            map.put("X-Content-Type-Options", "nosniff");

            /* OWASP - Clickjacking (prevents loading in frames) */
            map.put("X-Frame-Options", "deny");

            /* OWASP - disable cross domain policies */
            map.put("X-Permitted-Cross-Domain-Policies", "none");

            /* OWASP Referrer Policy */
            map.put("Referrer-Policy", "no-referrer");

            /*
             * OWASP - Modern designs say this should now be disabled, values of 1 etc.
             * actually create issues
             */
            map.put("X-XSS-Protection", "0");

            this.defaultHeaders = Collections.unmodifiableMap(map);
        }
        return this.defaultHeaders;
    }
}
