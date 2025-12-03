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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Servlet for user authentication - login and register.
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    
    private final UserDao userDao = new UserDao();
    private final Gson gson = new Gson();
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        
        try {
            if ("/login".equals(pathInfo)) {
                handleLogin(request, response, out);
            } else if ("/register".equals(pathInfo)) {
                handleRegister(request, response, out);
            } else if ("/logout".equals(pathInfo)) {
                handleLogout(request, response, out);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(createErrorResponse("Unknown endpoint"));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(createErrorResponse("Server error: " + e.getMessage()));
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        
        try {
            if ("/check".equals(pathInfo)) {
                handleCheckSession(request, response, out);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(createErrorResponse("Unknown endpoint"));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(createErrorResponse("Server error: " + e.getMessage()));
        }
    }
    
    private void handleLogin(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws Exception {
        
        JsonObject body = parseRequestBody(request);
        String username = body.has("username") ? body.get("username").getAsString() : null;
        String password = body.has("password") ? body.get("password").getAsString() : null;
        
        if (username == null || password == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createErrorResponse("Username and password are required"));
            return;
        }
        
        String hashedPassword = hashPassword(password);
        User user = userDao.authenticate(username, hashedPassword);
        
        if (user != null) {
            // Create session
            HttpSession session = request.getSession(true);
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());
            session.setMaxInactiveInterval(3600); // 1 hour
            
            // Don't return password in response
            user.setPassword(null);
            
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "Login successful");
            result.add("user", gson.toJsonTree(user));
            out.print(result);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(createErrorResponse("Invalid username or password"));
        }
    }
    
    private void handleRegister(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws Exception {
        
        JsonObject body = parseRequestBody(request);
        String username = body.has("username") ? body.get("username").getAsString() : null;
        String password = body.has("password") ? body.get("password").getAsString() : null;
        String phone = body.has("phone") ? body.get("phone").getAsString() : null;
        
        if (username == null || password == null || phone == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createErrorResponse("Username, password, and phone are required"));
            return;
        }
        
        // Check if username already exists
        if (userDao.findByUsername(username) != null) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            out.print(createErrorResponse("Username already exists"));
            return;
        }
        
        // Check if phone already exists
        if (userDao.findByPhone(phone) != null) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            out.print(createErrorResponse("Phone number already registered"));
            return;
        }
        
        User user = new User(username, hashPassword(password), phone);
        if (body.has("email")) {
            user.setEmail(body.get("email").getAsString());
        }
        
        Long userId = userDao.create(user);
        
        if (userId != null) {
            user.setId(userId);
            user.setPassword(null);
            
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "Registration successful");
            result.add("user", gson.toJsonTree(user));
            out.print(result);
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(createErrorResponse("Failed to create user"));
        }
    }
    
    private void handleLogout(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("message", "Logout successful");
        out.print(result);
    }
    
    private void handleCheckSession(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws Exception {
        
        HttpSession session = request.getSession(false);
        JsonObject result = new JsonObject();
        
        if (session != null && session.getAttribute("userId") != null) {
            Long userId = (Long) session.getAttribute("userId");
            User user = userDao.findById(userId);
            
            if (user != null) {
                user.setPassword(null);
                result.addProperty("loggedIn", true);
                result.add("user", gson.toJsonTree(user));
            } else {
                result.addProperty("loggedIn", false);
            }
        } else {
            result.addProperty("loggedIn", false);
        }
        
        out.print(result);
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
    
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    private String createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        return error.toString();
    }
}
