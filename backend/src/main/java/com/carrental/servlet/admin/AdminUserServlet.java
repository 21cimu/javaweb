package com.carrental.servlet.admin;

import com.carrental.dao.UserDao;
import com.carrental.model.User;
import com.carrental.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "AdminUserServlet", urlPatterns = {"/api/admin/users/*"})
public class AdminUserServlet extends HttpServlet {

    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo(); // e.g. /list or /detail/{id}
        if (path == null) {
            JsonUtil.writeError(resp, 404, "接口不存在");
            return;
        }

        if ("/list".equals(path)) {
            handleList(req, resp);
        } else if (path.startsWith("/detail/")) {
            String idStr = path.substring("/detail/".length());
            handleDetail(req, resp, idStr);
        } else {
            JsonUtil.writeError(resp, 404, "接口不存在");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if ("/approve".equals(path)) {
            handleApprove(req, resp);
        } else if ("/reject".equals(path)) {
            handleReject(req, resp);
        } else if ("/enable".equals(path)) {
            handleEnable(req, resp);
        } else if ("/disable".equals(path)) {
            handleDisable(req, resp);
        } else if ("/role".equals(path)) {
            handleUpdateRole(req, resp);
        } else {
            JsonUtil.writeError(resp, 404, "接口不存在");
        }
    }

    private void handleList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pageStr = req.getParameter("page");
        String pageSizeStr = req.getParameter("pageSize");
        String keyword = req.getParameter("keyword");
        String verStr = req.getParameter("verificationStatus");
        String role = req.getParameter("role");
        int page = 1, pageSize = 10;
        try {
            if (pageStr != null) page = Integer.parseInt(pageStr);
            if (pageSizeStr != null) pageSize = Integer.parseInt(pageSizeStr);
        } catch (NumberFormatException e) {
            // ignore
        }

        Integer verificationStatus = null;
        if (verStr != null && !verStr.isEmpty()) {
            try { verificationStatus = Integer.parseInt(verStr); } catch (NumberFormatException ignored) {}
        }

        List<User> list = userDao.searchByFilters(keyword, verificationStatus, role, page, pageSize);
        long total = userDao.countByFilters(keyword, verificationStatus, role);

        JsonUtil.writePaginated(resp, list, page, pageSize, total);
    }

    private void handleDetail(HttpServletRequest req, HttpServletResponse resp, String idStr) throws IOException {
        try {
            Long id = Long.parseLong(idStr);
            User user = userDao.findById(id);
            if (user == null) {
                JsonUtil.writeError(resp, 404, "用户不存在");
                return;
            }
            JsonUtil.writeSuccess(resp, user);
        } catch (NumberFormatException e) {
            JsonUtil.writeError(resp, 400, "无效的用户ID");
        }
    }

    private void handleApprove(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(req, Map.class);
        Number idNum = (Number) body.get("userId");
        if (idNum == null) {
            JsonUtil.writeError(resp, 400, "userId 不能为空");
            return;
        }
        Long userId = idNum.longValue();
        int r = userDao.updateVerificationStatus(userId, 2); // approved
        if (r > 0) JsonUtil.writeSuccess(resp, "已通过", null);
        else JsonUtil.writeError(resp, 500, "操作失败");
    }

    private void handleReject(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(req, Map.class);
        Number idNum = (Number) body.get("userId");
        if (idNum == null) {
            JsonUtil.writeError(resp, 400, "userId 不能为空");
            return;
        }
        Long userId = idNum.longValue();
        int r = userDao.updateVerificationStatus(userId, 3); // rejected
        if (r > 0) JsonUtil.writeSuccess(resp, "已拒绝", null);
        else JsonUtil.writeError(resp, 500, "操作失败");
    }

    private void handleEnable(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(req, Map.class);
        Number idNum = (Number) body.get("userId");
        if (idNum == null) {
            JsonUtil.writeError(resp, 400, "userId 不能为空");
            return;
        }
        Long userId = idNum.longValue();
        int r = userDao.updateStatus(userId, 1);
        if (r > 0) JsonUtil.writeSuccess(resp, "已启用", null);
        else JsonUtil.writeError(resp, 500, "操作失败");
    }

    private void handleDisable(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(req, Map.class);
        Number idNum = (Number) body.get("userId");
        if (idNum == null) {
            JsonUtil.writeError(resp, 400, "userId 不能为空");
            return;
        }
        Long userId = idNum.longValue();
        int r = userDao.updateStatus(userId, 0);
        if (r > 0) JsonUtil.writeSuccess(resp, "已禁用", null);
        else JsonUtil.writeError(resp, 500, "操作失败");
    }

    private void handleUpdateRole(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(req, Map.class);
        if (body == null) {
            JsonUtil.writeError(resp, 400, "请求体不能为空");
            return;
        }
        Object idObj = body.get("userId");
        Object roleObj = body.get("role");
        if (!(idObj instanceof Number)) {
            JsonUtil.writeError(resp, 400, "userId 不能为空");
            return;
        }
        String role = roleObj == null ? null : roleObj.toString().trim();
        if (role == null || role.isEmpty()) {
            JsonUtil.writeError(resp, 400, "role 不能为空");
            return;
        }
        if (!"user".equals(role) && !"admin".equals(role) && !"superadmin".equals(role)) {
            JsonUtil.writeError(resp, 400, "role 无效");
            return;
        }
        Long userId = ((Number) idObj).longValue();
        int r = userDao.updateRole(userId, role);
        if (r > 0) {
            JsonUtil.writeSuccess(resp, "权限已更新", null);
        } else {
            JsonUtil.writeError(resp, 404, "用户不存在");
        }
    }
}
