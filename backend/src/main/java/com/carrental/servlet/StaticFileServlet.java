package com.carrental.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;

// Serve both uploaded files and frontend public images
@WebServlet(name = "StaticFileServlet", urlPatterns = {"/uploads/*", "/images/*"})
public class StaticFileServlet extends HttpServlet {

    private static final String FRONTEND_IMAGES_DIR = "D:\\java\\javaweb\\frontend\\public\\images";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath(); // "/uploads" or "/images"
        String pathInfo = req.getPathInfo(); // /<filename or subpath>
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.length() < 2) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String filename = pathInfo.substring(1); // remove leading '/'
        File file;

        if ("/images".equals(servletPath)) {
            // serve from frontend public images directory
            File imagesDir = new File(FRONTEND_IMAGES_DIR);
            file = new File(imagesDir, filename);
        } else {
            // default: serve from workingDir/uploads
            String workingDir = new File(".").getCanonicalPath();
            File uploadsDir = new File(workingDir, "uploads");
            file = new File(uploadsDir, filename);
        }

        if (!file.exists() || !file.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String mimeType = URLConnection.guessContentTypeFromName(file.getName());
        if (mimeType == null) mimeType = "application/octet-stream";
        resp.setContentType(mimeType);
        resp.setContentLengthLong(file.length());

        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                resp.getOutputStream().write(buffer, 0, len);
            }
        }
    }
}
