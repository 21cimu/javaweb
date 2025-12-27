package com.carrental.filter;

import com.carrental.dao.AdminOperationLogDao;
import com.carrental.model.AdminOperationLog;
import com.carrental.util.JsonUtil;
import com.carrental.util.JwtUtil;
import com.carrental.util.LogContext;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Authentication Filter for JWT-based authentication
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = "/api/*")
public class AuthFilter implements Filter {

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/forgot-password",
        "/api/upload",
        "/uploads",
        "/api/vehicles/list",
        "/api/vehicles/detail",
        "/api/vehicles/search",
        "/api/vehicles/categories",
        "/api/reviews",
        "/api/stores/list",
        "/api/marketing/promotions",
        "/api/marketing/banners",
        // Alipay callbacks must be public (signature verification is done in servlet)
        "/api/pay/alipay/notify",
        "/api/pay/alipay/return"
    );

    private final AdminOperationLogDao adminOperationLogDao = new AdminOperationLogDao();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization required
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        LogContext.clear();
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String relativePath = path.substring(contextPath.length());
        String method = httpRequest.getMethod();

        // Skip authentication for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Check if path is public
        if (isPublicPath(relativePath)) {
            chain.doFilter(request, response);
            return;
        }

        // Get token from Authorization header
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            JsonUtil.writeError(httpResponse, 401, "未授权：缺少认证令牌");
            return;
        }

        String token = authHeader.substring(7);
        if (!JwtUtil.isTokenValid(token)) {
            JsonUtil.writeError(httpResponse, 401, "未授权：令牌无效或已过期");
            return;
        }

        Long userId = JwtUtil.getUserId(token);
        String username = JwtUtil.getUsername(token);
        String role = JwtUtil.getRole(token);

        // Check admin access for admin endpoints
        if (relativePath.startsWith("/api/admin/")) {
            if (!"admin".equals(role) && !"superadmin".equals(role)) {
                JsonUtil.writeError(httpResponse, 403, "禁止访问：需要管理员权限");
                return;
            }
        }

        // Set user info in request attributes
        httpRequest.setAttribute("userId", userId);
        httpRequest.setAttribute("username", username);
        httpRequest.setAttribute("role", role);
        LogContext.set(userId, username, role, getClientIp(httpRequest), httpRequest.getHeader("User-Agent"));

        boolean shouldAudit = relativePath.startsWith("/api/admin/")
            && !relativePath.startsWith("/api/admin/logs");
        StatusCaptureResponseWrapper wrappedResponse = shouldAudit
            ? new StatusCaptureResponseWrapper(httpResponse)
            : null;
        HttpServletResponse responseToUse = wrappedResponse != null ? wrappedResponse : httpResponse;

        try {
            chain.doFilter(request, responseToUse);
        } finally {
            if (shouldAudit && shouldLogMethod(method)) {
                int status = wrappedResponse != null ? wrappedResponse.getStatus() : httpResponse.getStatus();
                recordAdminLog(httpRequest, relativePath, status, userId, username, role);
            }
            LogContext.clear();
        }
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldLogMethod(String method) {
        if (method == null) return false;
        String upper = method.toUpperCase();
        return "POST".equals(upper) || "PUT".equals(upper) || "DELETE".equals(upper) || "PATCH".equals(upper);
    }

    private void recordAdminLog(HttpServletRequest request, String relativePath, int status,
            Long userId, String username, String role) {
        try {
            AdminOperationLog log = new AdminOperationLog();
            log.setOperatorId(userId);
            log.setOperatorName(username);
            log.setOperatorRole(role);
            log.setModule(resolveModule(relativePath));
            log.setAction(resolveAction(request.getMethod(), relativePath));
            log.setTarget(resolveTarget(relativePath));
            log.setResult(status >= 200 && status < 400 ? "success" : "fail");
            log.setIp(getClientIp(request));
            log.setRemark(resolveRemark(request));
            adminOperationLogDao.create(log);
        } catch (Exception ignored) {
            // Avoid breaking main flow if log insert fails.
        }
    }

    private String resolveModule(String relativePath) {
        if (relativePath.startsWith("/api/admin/orders")) return "orders";
        if (relativePath.startsWith("/api/admin/vehicles")) return "vehicles";
        if (relativePath.startsWith("/api/admin/users")) return "users";
        if (relativePath.startsWith("/api/admin/coupons")) return "marketing";
        if (relativePath.startsWith("/api/admin/user-coupons")) return "marketing";
        if (relativePath.startsWith("/api/admin/stores")) return "stores";
        if (relativePath.startsWith("/api/admin/reviews")) return "reviews";
        if (relativePath.startsWith("/api/admin/after-sales")) return "after-sales";
        if (relativePath.startsWith("/api/admin/dashboard")) return "dashboard";
        if (relativePath.startsWith("/api/admin/maintenance")) return "maintenance";
        return "admin";
    }

    private String resolveAction(String method, String relativePath) {
        String m = method == null ? "" : method.toUpperCase();
        return m + " " + relativePath;
    }

    private String resolveTarget(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return "";
        String[] parts = relativePath.split("/");
        if (parts.length == 0) return "";
        String last = parts[parts.length - 1];
        if (last.matches("\\d+")) {
            return last;
        }
        return "";
    }

    private String resolveRemark(HttpServletRequest request) {
        String query = request.getQueryString();
        return query != null && !query.isBlank() ? "query=" + query : "";
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            if (parts.length > 0 && !parts[0].isBlank()) {
                return parts[0].trim();
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static class StatusCaptureResponseWrapper extends HttpServletResponseWrapper {
        private int status = HttpServletResponse.SC_OK;

        StatusCaptureResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
            super.setStatus(sc);
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.status = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.status = sc;
            super.sendError(sc, msg);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            this.status = HttpServletResponse.SC_FOUND;
            super.sendRedirect(location);
        }

        public int getStatus() {
            return status;
        }
    }

    @Override
    public void destroy() {
        // No cleanup required
    }
}
