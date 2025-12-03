package com.carrental.filter;

import com.carrental.util.JsonUtil;
import com.carrental.util.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
        "/api/vehicles/list",
        "/api/vehicles/detail",
        "/api/vehicles/search",
        "/api/vehicles/categories",
        "/api/stores/list",
        "/api/marketing/promotions",
        "/api/marketing/banners"
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization required
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String relativePath = path.substring(contextPath.length());

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

        // Check admin access for admin endpoints
        if (relativePath.startsWith("/api/admin/")) {
            String role = JwtUtil.getRole(token);
            if (!"admin".equals(role) && !"superadmin".equals(role)) {
                JsonUtil.writeError(httpResponse, 403, "禁止访问：需要管理员权限");
                return;
            }
        }

        // Set user info in request attributes
        httpRequest.setAttribute("userId", JwtUtil.getUserId(token));
        httpRequest.setAttribute("username", JwtUtil.getUsername(token));
        httpRequest.setAttribute("role", JwtUtil.getRole(token));

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        // No cleanup required
    }
}
