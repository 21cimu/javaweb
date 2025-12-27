package com.carrental.servlet.review;

import com.carrental.dao.OrderDao;
import com.carrental.model.Order;
import com.carrental.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "ReviewServlet", urlPatterns = {"/api/reviews/*"})
public class ReviewServlet extends HttpServlet {

    private final OrderDao orderDao = new OrderDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo) || "/list".equals(pathInfo)) {
            handleList(request, response);
            return;
        }
        JsonUtil.writeError(response, 404, "接口不存在");
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String pageStr = request.getParameter("page");
        String pageSizeStr = request.getParameter("pageSize");
        String ratingStr = request.getParameter("rating");
        String vehicleIdStr = request.getParameter("vehicleId");

        if (vehicleIdStr == null || vehicleIdStr.isBlank()) {
            JsonUtil.writeError(response, 400, "vehicleId不能为空");
            return;
        }

        Long vehicleId;
        try {
            vehicleId = Long.parseLong(vehicleIdStr);
        } catch (NumberFormatException e) {
            JsonUtil.writeError(response, 400, "vehicleId格式不正确");
            return;
        }

        int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
        int pageSize = pageSizeStr != null ? Integer.parseInt(pageSizeStr) : 10;
        Integer rating = null;
        if (ratingStr != null && !ratingStr.isBlank()) {
            try {
                rating = Integer.parseInt(ratingStr);
            } catch (NumberFormatException e) {
                JsonUtil.writeError(response, 400, "rating格式不正确");
                return;
            }
        }

        List<Order> reviews = orderDao.findReviews(rating, vehicleId, null, page, pageSize);
        long total = orderDao.countReviews(rating, vehicleId, null);

        List<Map<String, Object>> sanitized = new ArrayList<>();
        for (Order order : reviews) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", order.getId());
            item.put("userName", normalizeUserName(order.getUserName()));
            item.put("rating", order.getRating());
            item.put("review", order.getReview());
            item.put("reviewImages", order.getReviewImages());
            item.put("reviewTime", order.getReviewTime());
            sanitized.add(item);
        }

        JsonUtil.writePaginated(response, sanitized, page, pageSize, total);
    }

    private String normalizeUserName(String name) {
        if (name == null || name.isBlank()) {
            return "匿名用户";
        }
        return name.trim();
    }
}
