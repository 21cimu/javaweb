package com.carrental.servlet.vehicle;

import com.carrental.dao.StoreDao;
import com.carrental.model.Store;
import com.carrental.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

/**
 * Store Servlet for store listing
 */
@WebServlet(name = "StoreServlet", urlPatterns = {"/api/stores/*"})
public class StoreServlet extends HttpServlet {

    private final StoreDao storeDao = new StoreDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || "/".equals(pathInfo) || "/list".equals(pathInfo)) {
            handleList(request, response);
        } else if ("/cities".equals(pathInfo)) {
            handleCities(request, response);
        } else if (pathInfo.startsWith("/detail/")) {
            handleDetail(request, response, pathInfo.substring(8));
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String city = request.getParameter("city");

        List<Store> stores;
        if (city != null && !city.isEmpty()) {
            stores = storeDao.findByCity(city);
        } else {
            stores = storeDao.findActive();
        }

        JsonUtil.writeSuccess(response, stores);
    }

    private void handleCities(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        List<String> cities = storeDao.findAllCities();
        JsonUtil.writeSuccess(response, cities);
    }

    private void handleDetail(HttpServletRequest request, HttpServletResponse response,
            String idStr) throws IOException {
        try {
            Long id = Long.parseLong(idStr);
            Store store = storeDao.findById(id);
            
            if (store == null) {
                JsonUtil.writeError(response, 404, "门店不存在");
                return;
            }

            JsonUtil.writeSuccess(response, store);
        } catch (NumberFormatException e) {
            JsonUtil.writeError(response, 400, "无效的门店ID");
        }
    }
}
