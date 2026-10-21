package com.example.demo;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple per-IP rate limiter applied to all /api/* routes.
 *
 * Why this exists:
 * - Rapid repeated requests (e.g., button-mashing "Refresh") can exhaust external API quotas
 *   or overwhelm the database connection pool.
 * - This filter tracks how many requests each IP has made in the current 60-second window.
 *   Once the limit is exceeded the request is rejected with 429 Too Many Requests and a
 *   human-readable JSON message instead of a cryptic error.
 *
 * Limits:
 * - 60 requests per IP per 60-second rolling window.
 *
 * Registered for /api/* only (see WebConfig) so that static frontend assets are unaffected.
 */
public class RateLimitFilter implements Filter {

    static final int MAX_REQUESTS_PER_WINDOW = 60;
    static final long WINDOW_MS = 60_000L;

    // Key: client IP address.  Value: [windowStartMs, requestCount]
    private final Map<String, long[]> windows = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpReq  = (HttpServletRequest)  request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        String ip = resolveClientIp(httpReq);

        // Localhost is never rate-limited (dev/demo traffic from the same machine)
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")) {
            chain.doFilter(request, response);
            return;
        }

        if (isOverLimit(ip)) {
            httpResp.setStatus(429);
            httpResp.setContentType("application/json");
            httpResp.getWriter().write(
                    "{\"error\":\"Too many requests \u2014 please slow down and wait a minute.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Returns true if the IP has exceeded MAX_REQUESTS_PER_WINDOW in the current window,
     * and increments the counter as a side effect.
     */
    boolean isOverLimit(String ip) {
        long now = System.currentTimeMillis();

        windows.compute(ip, (key, entry) -> {
            if (entry == null || now - entry[0] >= WINDOW_MS) {
                return new long[]{now, 1};
            }
            entry[1]++;
            return entry;
        });

        long[] entry = windows.get(ip);
        return entry != null && entry[1] > MAX_REQUESTS_PER_WINDOW;
    }

    /** Prefers X-Forwarded-For so the filter works correctly behind a reverse proxy. */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
