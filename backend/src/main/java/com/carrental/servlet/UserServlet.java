package com.carrental.servlet;

import com.carrental.dao.UserDao;
import com.carrental.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Servlet for user management operations.
 */
@WebServlet("/api/users/*")
public class UserServlet extends HttpServlet {
    
    private final UserDao userDao = new UserDao();
    private final Gson gson = new Gson();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Get all users
                List<User> users = userDao.findAll();
                // Remove passwords from response
                users.forEach(u -> u.setPassword(null));
                out.print(gson.toJson(users));
            } else {
                // Get user by ID
                String idStr = pathInfo.substring(1);
                Long id = Long.parseLong(idStr);
                User user = userDao.findById(id);
                
                if (user != null) {
                    user.setPassword(null);
                    out.print(gson.toJson(user));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(createErrorResponse("User not found"));
                }
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createErrorResponse("Invalid user ID"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(createErrorResponse("Server error: " + e.getMessage()));
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createErrorResponse("User ID is required"));
            return;
        }
        
        try {
            String idStr = pathInfo.substring(1);
            Long id = Long.parseLong(idStr);
            
            User existingUser = userDao.findById(id);
            if (existingUser == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(createErrorResponse("User not found"));
                return;
            }
            
            JsonObject body = parseRequestBody(request);
            
            // Update fields if provided
            if (body.has("phone")) existingUser.setPhone(body.get("phone").getAsString());
            if (body.has("email")) existingUser.setEmail(body.get("email").getAsString());
            if (body.has("realName")) existingUser.setRealName(body.get("realName").getAsString());
            if (body.has("idCard")) existingUser.setIdCard(body.get("idCard").getAsString());
            if (body.has("drivingLicense")) existingUser.setDrivingLicense(body.get("drivingLicense").getAsString());
            
            boolean success = userDao.update(existingUser);
            
            if (success) {
                existingUser.setPassword(null);
                JsonObject result = new JsonObject();
                result.addProperty("success", true);
                result.addProperty("message", "User updated successfully");
                result.add("user", gson.toJsonTree(existingUser));
                out.print(result);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print(createErrorResponse("Failed to update user"));
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createErrorResponse("Invalid user ID"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(createErrorResponse("Server error: " + e.getMessage()));
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createErrorResponse("User ID is required"));
            return;
        }
        
        try {
            String idStr = pathInfo.substring(1);
            Long id = Long.parseLong(idStr);
            
            boolean success = userDao.delete(id);
            
            if (success) {
                JsonObject result = new JsonObject();
                result.addProperty("success", true);
                result.addProperty("message", "User deleted successfully");
                out.print(result);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(createErrorResponse("User not found"));
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createErrorResponse("Invalid user ID"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(createErrorResponse("Server error: " + e.getMessage()));
        }
    }
    
    private JsonObject parseRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return gson.fromJson(sb.toString(), JsonObject.class);
    }
    
    private String createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        return error.toString();
    }
}
