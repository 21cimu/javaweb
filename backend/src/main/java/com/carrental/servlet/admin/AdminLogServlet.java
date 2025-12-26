package com.carrental.servlet.admin;

import com.carrental.dao.AdminOperationLogDao;
import com.carrental.dao.FundsFlowLogDao;
import com.carrental.dao.LoginSecurityLogDao;
import com.carrental.dao.OrderEventLogDao;
import com.carrental.dao.VehicleStatusLogDao;
import com.carrental.model.AdminOperationLog;
import com.carrental.model.FundsFlowLog;
import com.carrental.model.LoginSecurityLog;
import com.carrental.model.OrderEventLog;
import com.carrental.model.VehicleStatusLog;
import com.carrental.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@WebServlet(name = "AdminLogServlet", urlPatterns = {"/api/admin/logs/*"})
public class AdminLogServlet extends HttpServlet {

    private final LoginSecurityLogDao loginSecurityLogDao = new LoginSecurityLogDao();
    private final AdminOperationLogDao adminOperationLogDao = new AdminOperationLogDao();
    private final OrderEventLogDao orderEventLogDao = new OrderEventLogDao();
    private final FundsFlowLogDao fundsFlowLogDao = new FundsFlowLogDao();
    private final VehicleStatusLogDao vehicleStatusLogDao = new VehicleStatusLogDao();

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if ("/login-security".equals(pathInfo)) {
            handleLoginSecurity(request, response);
        } else if ("/operation-audit".equals(pathInfo)) {
            handleOperationAudit(request, response);
        } else if ("/order-events".equals(pathInfo)) {
            handleOrderEvents(request, response);
        } else if ("/funds-flow".equals(pathInfo)) {
            handleFundsFlow(request, response);
        } else if ("/vehicle-status".equals(pathInfo)) {
            handleVehicleStatus(request, response);
        } else {
            JsonUtil.writeError(response, 404, "not_found");
        }
    }

    private void handleLoginSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int page = parseInt(request.getParameter("page"), 1);
        int pageSize = parseInt(request.getParameter("pageSize"), 10);
        String account = request.getParameter("account");
        String result = request.getParameter("result");
        LocalDateTime startDate = parseStartDate(request.getParameter("startDate"));
        LocalDateTime endDate = parseEndDate(request.getParameter("endDate"));

        List<LoginSecurityLog> list = loginSecurityLogDao.findAll(account, result, startDate, endDate, page, pageSize);
        long total = loginSecurityLogDao.countAll(account, result, startDate, endDate);
        JsonUtil.writePaginated(response, list, page, pageSize, total);
    }

    private void handleOperationAudit(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int page = parseInt(request.getParameter("page"), 1);
        int pageSize = parseInt(request.getParameter("pageSize"), 10);
        String operator = request.getParameter("operator");
        String module = request.getParameter("module");
        String result = request.getParameter("result");
        LocalDateTime startDate = parseStartDate(request.getParameter("startDate"));
        LocalDateTime endDate = parseEndDate(request.getParameter("endDate"));

        List<AdminOperationLog> list = adminOperationLogDao.findAll(operator, module, result, startDate, endDate, page, pageSize);
        long total = adminOperationLogDao.countAll(operator, module, result, startDate, endDate);
        JsonUtil.writePaginated(response, list, page, pageSize, total);
    }

    private void handleOrderEvents(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int page = parseInt(request.getParameter("page"), 1);
        int pageSize = parseInt(request.getParameter("pageSize"), 10);
        String orderNo = request.getParameter("orderNo");
        String eventType = request.getParameter("eventType");
        String operator = request.getParameter("operator");
        LocalDateTime startDate = parseStartDate(request.getParameter("startDate"));
        LocalDateTime endDate = parseEndDate(request.getParameter("endDate"));

        List<OrderEventLog> list = orderEventLogDao.findAll(orderNo, eventType, operator, startDate, endDate, page, pageSize);
        long total = orderEventLogDao.countAll(orderNo, eventType, operator, startDate, endDate);
        JsonUtil.writePaginated(response, list, page, pageSize, total);
    }

    private void handleFundsFlow(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int page = parseInt(request.getParameter("page"), 1);
        int pageSize = parseInt(request.getParameter("pageSize"), 10);
        String keyword = request.getParameter("keyword");
        String type = request.getParameter("type");
        String channel = request.getParameter("channel");
        LocalDateTime startDate = parseStartDate(request.getParameter("startDate"));
        LocalDateTime endDate = parseEndDate(request.getParameter("endDate"));

        List<FundsFlowLog> list = fundsFlowLogDao.findAll(keyword, type, channel, startDate, endDate, page, pageSize);
        long total = fundsFlowLogDao.countAll(keyword, type, channel, startDate, endDate);
        JsonUtil.writePaginated(response, list, page, pageSize, total);
    }

    private void handleVehicleStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int page = parseInt(request.getParameter("page"), 1);
        int pageSize = parseInt(request.getParameter("pageSize"), 10);
        String vehicle = request.getParameter("vehicle");
        Integer status = parseInteger(request.getParameter("status"));
        LocalDateTime startDate = parseStartDate(request.getParameter("startDate"));
        LocalDateTime endDate = parseEndDate(request.getParameter("endDate"));

        List<VehicleStatusLog> list = vehicleStatusLogDao.findAll(vehicle, status, startDate, endDate, page, pageSize);
        long total = vehicleStatusLogDao.countAll(vehicle, status, startDate, endDate);
        JsonUtil.writePaginated(response, list, page, pageSize, total);
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return value == null ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseStartDate(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.length() == 10) {
            LocalDate d = LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return d.atStartOfDay();
        }
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    private LocalDateTime parseEndDate(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.length() == 10) {
            LocalDate d = LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return d.atTime(23, 59, 59);
        }
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }
}
