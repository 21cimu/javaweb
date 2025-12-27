package com.carrental.servlet.user;

import com.carrental.dao.UserDao;
import com.carrental.model.User;
import com.carrental.util.JsonUtil;
import com.carrental.util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

/**
 * User Profile Servlet for user center operations
 */
@WebServlet(name = "UserServlet", urlPatterns = {"/api/user/*"})
public class UserServlet extends HttpServlet {

    private final UserDao userDao = new UserDao();
    private static final String FRONTEND_IMAGES_DIR = "D:\\java\\javaweb\\frontend\\public\\images";
    private static final int CAPTCHA_LENGTH = 4;
    private static final int CAPTCHA_WIDTH = 120;
    private static final int CAPTCHA_HEIGHT = 40;
    private static final long CAPTCHA_TTL_MS = 2 * 60 * 1000L;
    private static final SecureRandom CAPTCHA_RANDOM = new SecureRandom();
    private static final ConcurrentHashMap<Long, CaptchaEntry> PASSWORD_CAPTCHA_STORE = new ConcurrentHashMap<>();

    private static class CaptchaEntry {
        private final String code;
        private final long expiresAt;

        private CaptchaEntry(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

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
        } else if ("/password-captcha".equals(pathInfo)) {
            handlePasswordCaptcha(response, userId);
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

        String oldAvatar = user.getAvatar();
        boolean avatarProvided = false;
        String newAvatar = null;

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
            avatarProvided = true;
            newAvatar = (String) body.get("avatar");
            user.setAvatar(newAvatar);
        }
        if (body.containsKey("gender")) {
            Object gender = body.get("gender");
            if (gender instanceof Number) {
                user.setGender(((Number) gender).intValue());
            }
        }

        int result = userDao.update(user);
        if (result > 0) {
            if (avatarProvided && oldAvatar != null && !oldAvatar.isEmpty()) {
                if (newAvatar == null || newAvatar.isEmpty() || !oldAvatar.equals(newAvatar)) {
                    deleteOldAvatarIfNeeded(oldAvatar);
                }
            }
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
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeError(response, 500, "提交失败: " + e.getMessage());
        }
    }

    private void handlePasswordCaptcha(HttpServletResponse response, Long userId) throws IOException {
        String code = generateCaptchaCode();
        String imageBase64 = buildCaptchaImageBase64(code);
        if (imageBase64 == null) {
            JsonUtil.writeError(response, 500, "验证码生成失败");
            return;
        }
        PASSWORD_CAPTCHA_STORE.put(userId, new CaptchaEntry(code, System.currentTimeMillis() + CAPTCHA_TTL_MS));

        Map<String, Object> payload = new HashMap<>();
        payload.put("image", "data:image/png;base64," + imageBase64);
        payload.put("expiresIn", CAPTCHA_TTL_MS / 1000);
        JsonUtil.writeSuccess(response, payload);
    }

    private String generateCaptchaCode() {
        StringBuilder sb = new StringBuilder(CAPTCHA_LENGTH);
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            sb.append(CAPTCHA_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private String buildCaptchaImageBase64(String code) {
        BufferedImage image = new BufferedImage(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, CAPTCHA_WIDTH, CAPTCHA_HEIGHT);

            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            FontMetrics fm = g.getFontMetrics();
            int charSpace = CAPTCHA_WIDTH / (CAPTCHA_LENGTH + 1);

            for (int i = 0; i < code.length(); i++) {
                g.setColor(new Color(50 + CAPTCHA_RANDOM.nextInt(150),
                    50 + CAPTCHA_RANDOM.nextInt(150),
                    50 + CAPTCHA_RANDOM.nextInt(150)));
                String ch = String.valueOf(code.charAt(i));
                int x = charSpace * (i + 1) - fm.charWidth(ch.charAt(0)) / 2;
                int y = (CAPTCHA_HEIGHT + fm.getAscent() - fm.getDescent()) / 2;
                g.drawString(ch, x, y);
            }

            g.setColor(new Color(180, 180, 180));
            for (int i = 0; i < 6; i++) {
                int x1 = CAPTCHA_RANDOM.nextInt(CAPTCHA_WIDTH);
                int y1 = CAPTCHA_RANDOM.nextInt(CAPTCHA_HEIGHT);
                int x2 = CAPTCHA_RANDOM.nextInt(CAPTCHA_WIDTH);
                int y2 = CAPTCHA_RANDOM.nextInt(CAPTCHA_HEIGHT);
                g.drawLine(x1, y1, x2, y2);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            System.err.println("buildCaptchaImageBase64 error: " + e.getMessage());
            return null;
        } finally {
            g.dispose();
        }
    }

    private boolean validatePasswordCaptcha(Long userId, String input) {
        if (input == null) return false;
        CaptchaEntry entry = PASSWORD_CAPTCHA_STORE.get(userId);
        if (entry == null) return false;
        if (System.currentTimeMillis() > entry.expiresAt) {
            PASSWORD_CAPTCHA_STORE.remove(userId);
            return false;
        }
        boolean matched = entry.code.equalsIgnoreCase(input.trim());
        if (!matched) {
            PASSWORD_CAPTCHA_STORE.remove(userId);
            return false;
        }
        PASSWORD_CAPTCHA_STORE.remove(userId);
        return true;
    }

    private void handleChangePassword(HttpServletRequest request, HttpServletResponse response,
            Long userId) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        String oldPassword = (String) body.get("oldPassword");
        String newPassword = (String) body.get("newPassword");
        String captcha = (String) body.get("captcha");

        if (oldPassword == null || newPassword == null) {
            JsonUtil.writeError(response, 400, "请填写旧密码和新密码");
            return;
        }
        if (captcha == null || captcha.isBlank()) {
            JsonUtil.writeError(response, 400, "请输入验证码");
            return;
        }
        if (!validatePasswordCaptcha(userId, captcha)) {
            JsonUtil.writeError(response, 400, "验证码错误或已过期");
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
        if (!PasswordUtil.verifyPassword(oldPassword, user.getPassword())) {
            JsonUtil.writeError(response, 401, "旧密码错误");
            return;
        }

        String hashedNew = PasswordUtil.hashPassword(newPassword);
        int result = userDao.updatePassword(userId, hashedNew);

        if (result > 0) {
            JsonUtil.writeSuccess(response, "密码修改成功", null);
        } else {
            JsonUtil.writeError(response, 500, "修改失败");
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
        // VIP related fields
        result.put("vipLevel", user.getVipLevel());
        result.put("cumulativeSpending", user.getCumulativeSpending());
         return result;
     }

    private void deleteOldAvatarIfNeeded(String avatarPath) {
        try {
            if (avatarPath == null || avatarPath.isEmpty()) return;
            String normalized = avatarPath.trim().replace("\\", "/");
            String relative = null;
            String prefix1 = "/images/header/";
            String prefix2 = "images/header/";
            String prefix3 = "header/";
            if (normalized.startsWith(prefix1)) {
                relative = normalized.substring(prefix1.length());
            } else if (normalized.startsWith(prefix2)) {
                relative = normalized.substring(prefix2.length());
            } else if (normalized.startsWith(prefix3)) {
                relative = normalized.substring(prefix3.length());
            }
            if (relative == null || relative.isEmpty()) return;

            File headerDir = new File(FRONTEND_IMAGES_DIR, "header");
            File target = new File(headerDir, relative);
            String headerPath = headerDir.getCanonicalPath();
            String targetPath = target.getCanonicalPath();
            if (!targetPath.startsWith(headerPath + File.separator)) return;
            if (target.exists() && target.isFile()) {
                boolean deleted = target.delete();
                if (!deleted) {
                    System.err.println("Failed to delete old avatar: " + target.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            System.err.println("deleteOldAvatarIfNeeded error: " + e.getMessage());
        }
    }
}
