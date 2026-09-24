package com.lexhive.libraryapp.config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(cachedRequest, response);
        } finally {
            long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
            String params = request.getQueryString() == null ? "" : request.getQueryString();
            log.info("method={} url={} params={} body={} status={} latencyMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    params,
                    getStringBody(cachedRequest),
                    response.getStatus(),
                    latencyMs);
        }
    }

    private static String getStringBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }

        String encoding = request.getCharacterEncoding();
        Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
        String raw = new String(content, charset);

        try {
            return JSON.writeValueAsString(JSON.readTree(raw));
        } catch (IOException ignored) {
            return raw.replaceAll("\\s*\\R\\s*", " ").trim();
        }
    }
}
