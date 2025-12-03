package com.carrental.servlet.user;

import com.carrental.dao.UserDao;
import com.carrental.model.User;
import com.carrental.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * User Profile Servlet for user center operations
 */
@WebServlet(name = "UserServlet", urlPatterns = {"/api/user/*"})
public class UserServlet extends HttpServlet {

    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            JsonUtil.writeError(response, 401, "请先登录");
            return;
        }

        if ("/profile".equals(pathInfo)) {
            handleGetProfile(request, response, userId);
        } else if ("/verification".equals(pathInfo)) {
            handleGetVerification(request, response, userId);
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            JsonUtil.writeError(response, 401, "请先登录");
            return;
        }

        if ("/profile".equals(pathInfo)) {
            handleUpdateProfile(request, response, userId);
        } else if ("/verification".equals(pathInfo)) {
            handleSubmitVerification(request, response, userId);
        } else if ("/password".equals(pathInfo)) {
            handleChangePassword(request, response, userId);
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    private void handleGetProfile(HttpServletRequest request, HttpServletResponse response, 
            Long userId) throws IOException {
        User user = userDao.findById(userId);
        if (user == null) {
            JsonUtil.writeError(response, 404, "用户不存在");
            return;
        }

        JsonUtil.writeSuccess(response, sanitizeUser(user));
    }

    private void handleUpdateProfile(HttpServletRequest request, HttpServletResponse response,
            Long userId) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        
        User user = userDao.findById(userId);
        if (user == null) {
            JsonUtil.writeError(response, 404, "用户不存在");
            return;
        }

        // Update fields
        if (body.containsKey("email")) {
            String email = (String) body.get("email");
            if (email != null && !email.isEmpty()) {
                User existing = userDao.findByEmail(email);
                if (existing != null && !existing.getId().equals(userId)) {
                    JsonUtil.writeError(response, 400, "邮箱已被使用");
                    return;
                }
                user.setEmail(email);
            }
        }
        if (body.containsKey("phone")) {
            String phone = (String) body.get("phone");
            if (phone != null && !phone.isEmpty()) {
                if (!phone.matches("^1[3-9]\\d{9}$")) {
                    JsonUtil.writeError(response, 400, "请输入有效的手机号");
                    return;
                }
                User existing = userDao.findByPhone(phone);
                if (existing != null && !existing.getId().equals(userId)) {
                    JsonUtil.writeError(response, 400, "手机号已被使用");
                    return;
                }
                user.setPhone(phone);
            }
        }
        if (body.containsKey("realName")) {
            user.setRealName((String) body.get("realName"));
        }
        if (body.containsKey("avatar")) {
            user.setAvatar((String) body.get("avatar"));
        }
        if (body.containsKey("gender")) {
            Object gender = body.get("gender");
            if (gender instanceof Number) {
                user.setGender(((Number) gender).intValue());
            }
        }

        int result = userDao.update(user);
        if (result > 0) {
            JsonUtil.writeSuccess(response, "更新成功", sanitizeUser(user));
        } else {
            JsonUtil.writeError(response, 500, "更新失败");
        }
    }

    private void handleGetVerification(HttpServletRequest request, HttpServletResponse response,
            Long userId) throws IOException {
        User user = userDao.findById(userId);
        if (user == null) {
            JsonUtil.writeError(response, 404, "用户不存在");
            return;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("verificationStatus", user.getVerificationStatus());
        result.put("realName", user.getRealName());
        result.put("idCard", maskIdCard(user.getIdCard()));
        result.put("driverLicense", user.getDriverLicense());
        result.put("idCardFront", user.getIdCardFront());
        result.put("idCardBack", user.getIdCardBack());
        result.put("driverLicenseImage", user.getDriverLicenseImage());

        JsonUtil.writeSuccess(response, result);
    }

    private void handleSubmitVerification(HttpServletRequest request, HttpServletResponse response,
            Long userId) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);

        String realName = (String) body.get("realName");
        String idCard = (String) body.get("idCard");
        String driverLicense = (String) body.get("driverLicense");
        String idCardFront = (String) body.get("idCardFront");
        String idCardBack = (String) body.get("idCardBack");
        String driverLicenseImage = (String) body.get("driverLicenseImage");

        // Validation
        if (realName == null || realName.isEmpty()) {
            JsonUtil.writeError(response, 400, "请填写真实姓名");
            return;
        }
        if (idCard == null || !idCard.matches("^\\d{17}[\\dXx]$")) {
            JsonUtil.writeError(response, 400, "请填写有效的身份证号");
            return;
        }
        if (driverLicense == null || driverLicense.isEmpty()) {
            JsonUtil.writeError(response, 400, "请填写驾驶证号");
            return;
        }

        int result = userDao.updateVerification(userId, realName, idCard,
            driverLicense, driverLicenseImage, idCardFront, idCardBack, 1);

        if (result > 0) {
            JsonUtil.writeSuccess(response, "提交成功，等待审核", null);
        } else {
            JsonUtil.writeError(response, 500, "提交失败");
        }
    }

    private void handleChangePassword(HttpServletRequest request, HttpServletResponse response,
            Long userId) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        String oldPassword = (String) body.get("oldPassword");
        String newPassword = (String) body.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            JsonUtil.writeError(response, 400, "请填写旧密码和新密码");
            return;
        }
        if (newPassword.length() < 6) {
            JsonUtil.writeError(response, 400, "新密码至少6个字符");
            return;
        }

        User user = userDao.findById(userId);
        if (user == null) {
            JsonUtil.writeError(response, 404, "用户不存在");
            return;
        }

        // Verify old password
        String hashedOld = hashPassword(oldPassword);
        if (!hashedOld.equals(user.getPassword())) {
            JsonUtil.writeError(response, 401, "旧密码错误");
            return;
        }

        String hashedNew = hashPassword(newPassword);
        int result = userDao.updatePassword(userId, hashedNew);

        if (result > 0) {
            JsonUtil.writeSuccess(response, "密码修改成功", null);
        } else {
            JsonUtil.writeError(response, 500, "修改失败");
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
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
}
