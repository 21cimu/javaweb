package com.carrental.servlet.admin;

import com.carrental.dao.OrderDao;
import com.carrental.model.Order;
import com.carrental.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "AdminReviewServlet", urlPatterns = {"/api/admin/reviews/*"})
public class AdminReviewServlet extends HttpServlet {

    private final OrderDao orderDao = new OrderDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // GET /api/admin/reviews?page=1&pageSize=20&rating=5&vehicleId=123&userId=456
        String pageStr = request.getParameter("page");
        String pageSizeStr = request.getParameter("pageSize");
        String ratingStr = request.getParameter("rating");
        String vehicleIdStr = request.getParameter("vehicleId");
        String userIdStr = request.getParameter("userId");

        int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
        int pageSize = pageSizeStr != null ? Integer.parseInt(pageSizeStr) : 20;
        Integer rating = ratingStr != null ? Integer.parseInt(ratingStr) : null;
        Long vehicleId = vehicleIdStr != null ? Long.parseLong(vehicleIdStr) : null;
        Long userId = userIdStr != null ? Long.parseLong(userIdStr) : null;

        List<Order> reviews = orderDao.findReviews(rating, vehicleId, userId, page, pageSize);
        long total = orderDao.countReviews(rating, vehicleId, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("data", reviews);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", total);

        JsonUtil.writeSuccess(response, result);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // DELETE /api/admin/reviews/{orderId}
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            JsonUtil.writeError(response, 400, "缺少订单ID");
            return;
        }
        String idStr = pathInfo.substring(1);
        Long orderId;
        try {
            orderId = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            JsonUtil.writeError(response, 400, "订单ID无效");
            return;
        }

        Order order = orderDao.findById(orderId);
        if (order == null) {
            JsonUtil.writeError(response, 404, "订单不存在");
            return;
        }

        if (order.getReview() == null || order.getReview().isEmpty()) {
            JsonUtil.writeError(response, 400, "订单未包含评论");
            return;
        }

        int result = orderDao.clearReview(orderId);
        if (result > 0) {
            JsonUtil.writeSuccess(response, "评论已删除", null);
        } else {
            JsonUtil.writeError(response, 500, "删除失败");
        }
    }
}

