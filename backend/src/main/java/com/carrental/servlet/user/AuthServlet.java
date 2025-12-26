package com.carrental.servlet.user;

import com.carrental.dao.LoginSecurityLogDao;
import com.carrental.dao.UserDao;
import com.carrental.model.LoginSecurityLog;
import com.carrental.model.User;
import com.carrental.util.JsonUtil;
import com.carrental.util.JwtUtil;
import com.carrental.util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication Servlet for user login and registration
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth/*"})
public class AuthServlet extends HttpServlet {
    
    private final UserDao userDao = new UserDao();
    private final LoginSecurityLogDao loginSecurityLogDao = new LoginSecurityLogDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        if ("/login".equals(pathInfo)) {
            handleLogin(request, response);
        } else if ("/register".equals(pathInfo)) {
            handleRegister(request, response);
        } else if ("/forgot-password".equals(pathInfo)) {
            handleForgotPassword(request, response);
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        String username = (String) body.get("username");
        String password = (String) body.get("password");

        if (username == null || password == null) {
            logLoginAttempt(request, null, username, "fail", "missing_credentials");
            JsonUtil.writeError(response, 400, "用户名和密码不能为空");
            return;
        }

        // Try to find user by username, phone, or email
        User user = userDao.findByUsername(username);
        if (user == null) {
            user = userDao.findByPhone(username);
        }
        if (user == null) {
            user = userDao.findByEmail(username);
        }

        if (user == null) {
            logLoginAttempt(request, null, username, "fail", "user_not_found");
            JsonUtil.writeError(response, 401, "用户不存在");
            return;
        }

        // Verify password
        if (!PasswordUtil.verifyPassword(password, user.getPassword())) {
            logLoginAttempt(request, user, username, "fail", "invalid_password");
            JsonUtil.writeError(response, 401, "密码错误");
            return;
        }

        if (user.getStatus() != 1) {
            logLoginAttempt(request, user, username, "fail", "account_disabled");
            JsonUtil.writeError(response, 403, "账号已被禁用");
            return;
        }

        // Generate token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", sanitizeUser(user));

        logLoginAttempt(request, user, username, "success", "login_success");
        JsonUtil.writeSuccess(response, "登录成功", result);
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String phone = (String) body.get("phone");
        String inviteCode = (String) body.get("inviteCode");

        // Validation
        if (username == null || username.length() < 3) {
            JsonUtil.writeError(response, 400, "用户名至少3个字符");
            return;
        }
        if (password == null || password.length() < 6) {
            JsonUtil.writeError(response, 400, "密码至少6个字符");
            return;
        }
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            JsonUtil.writeError(response, 400, "请输入有效的手机号");
            return;
        }

        // Check if username exists
        if (userDao.findByUsername(username) != null) {
            JsonUtil.writeError(response, 400, "用户名已存在");
            return;
        }

        // Check if phone exists
        if (userDao.findByPhone(phone) != null) {
            JsonUtil.writeError(response, 400, "手机号已注册");
            return;
        }

        // Create user
        User user = new User(username, PasswordUtil.hashPassword(password), phone);
        user.setInviteCode(generateInviteCode());
        user.setBalance(BigDecimal.ZERO);
        user.setPoints(0);

        // Handle invite code
        if (inviteCode != null && !inviteCode.isEmpty()) {
            User inviter = userDao.findByInviteCode(inviteCode);
            if (inviter != null) {
                user.setInviterId(inviter.getId());
                // Award points to inviter
                userDao.updatePoints(inviter.getId(), 100);
            }
        }

        Long userId = userDao.create(user);
        if (userId == null) {
            JsonUtil.writeError(response, 500, "注册失败，请稍后重试");
            return;
        }

        user.setId(userId);
        String token = JwtUtil.generateToken(userId, username, "user");

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", sanitizeUser(user));

        JsonUtil.writeSuccess(response, "注册成功", result);
    }

    private void handleForgotPassword(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        String phone = (String) body.get("phone");

        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            JsonUtil.writeError(response, 400, "请输入有效的手机号");
            return;
        }

        User user = userDao.findByPhone(phone);
        if (user == null) {
            JsonUtil.writeError(response, 404, "该手机号未注册");
            return;
        }

        // In production, send SMS with verification code
        // For demo, just return success
        JsonUtil.writeSuccess(response, "验证码已发送", null);
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Map<String, Object> sanitizeUser(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("phone", user.getPhone());
        result.put("email", user.getEmail());
        result.put("realName", user.getRealName());
        result.put("avatar", user.getAvatar());
        result.put("gender", user.getGender());
        result.put("verificationStatus", user.getVerificationStatus());
        result.put("role", user.getRole());
        result.put("balance", user.getBalance());
        result.put("points", user.getPoints());
        result.put("inviteCode", user.getInviteCode());
        return result;
    }

    private void logLoginAttempt(HttpServletRequest request, User user, String account,
            String result, String message) {
        try {
            LoginSecurityLog log = new LoginSecurityLog();
            log.setUserId(user != null ? user.getId() : null);
            log.setAccount(account);
            log.setIp(getClientIp(request));
            log.setLocation(null);
            log.setDevice(request.getHeader("User-Agent"));
            log.setResult(result);
            log.setMessage(message);
            loginSecurityLogDao.create(log);
        } catch (Exception ignored) {
            // Avoid breaking login flow if logging fails.
        }
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
}
