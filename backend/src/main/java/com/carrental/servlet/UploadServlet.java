package com.carrental.servlet;

import com.carrental.util.JsonUtil;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.UUID;

@WebServlet(name = "UploadServlet", urlPatterns = {"/api/upload"})
@MultipartConfig(fileSizeThreshold = 1024 * 1024, // 1MB
        maxFileSize = 10 * 1024 * 1024, // 10MB
        maxRequestSize = 50 * 1024 * 1024)
public class UploadServlet extends HttpServlet {

    // Preferred frontend images directory (as requested)
    private static final String FRONTEND_IMAGES_DIR = "D:\\java\\javaweb\\frontend\\public\\images";
    private static final String DEFAULT_SUBDIR = "message"; // default subdir when not provided
    // Fallback uploads directory (working dir/uploads)
    private static final String UPLOAD_ROOT_DIR = "uploads";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Part filePart = req.getPart("file");
            if (filePart == null) {
                JsonUtil.writeError(resp, 400, "没有上传文件");
                return;
            }

            String submittedFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            String ext = "";
            int idx = submittedFileName.lastIndexOf('.');
            if (idx > 0) ext = submittedFileName.substring(idx);

            // Determine desired base name from request (plateNumber or baseName), sanitize it
            String baseFromClient = null;
            String plateParam = req.getParameter("plateNumber");
            String baseParam = req.getParameter("baseName");
            if (plateParam != null && !plateParam.isEmpty()) baseFromClient = plateParam;
            else if (baseParam != null && !baseParam.isEmpty()) baseFromClient = baseParam;

            String sanitizedBase = null;
            if (baseFromClient != null) {
                // allow letters, numbers, '-', '_' and remove spaces and other chars
                sanitizedBase = baseFromClient.replaceAll("[^a-zA-Z0-9_-]", "");
                if (sanitizedBase.isEmpty()) sanitizedBase = null;
                // limit length to avoid very long filenames
                if (sanitizedBase != null && sanitizedBase.length() > 64) {
                    sanitizedBase = sanitizedBase.substring(0, 64);
                }
            }

            String fileName;
            if (sanitizedBase != null) {
                // If plate number was provided, use the plate as the filename (no UUID) so it can be referenced by plate
                if (plateParam != null && !plateParam.isEmpty()) {
                    fileName = sanitizedBase + ext; // e.g. HABC123.jpg
                } else {
                    // use base name + uuid to avoid overwriting existing files when baseName is used
                    fileName = sanitizedBase + "_" + UUID.randomUUID() + ext;
                }
            } else {
                fileName = UUID.randomUUID() + ext;
            }

            // read optional folder parameter to decide subdirectory under frontend/public/images
            String folderParam = req.getParameter("folder");
            String subdir = DEFAULT_SUBDIR;
            if (folderParam != null && !folderParam.isEmpty()) {
                // sanitize folder name: allow letters, numbers, '-', '_'
                String cleaned = folderParam.replaceAll("[^a-zA-Z0-9_-]", "");
                if (!cleaned.isEmpty()) {
                    subdir = cleaned;
                }
            } else if (plateParam != null && !plateParam.isEmpty()) {
                // if uploading with plate number and no folder specified, store under 'vehicles'
                subdir = "vehicles";
            }

            // Try to save into frontend public images/<subdir> folder if available
            File frontendImagesDir = new File(FRONTEND_IMAGES_DIR);
            File frontendSubDir = new File(frontendImagesDir, subdir);
            File outFile;
            if (frontendSubDir.exists() && frontendSubDir.isDirectory() && frontendSubDir.canWrite()) {
                outFile = new File(frontendSubDir, fileName);
            } else if (frontendImagesDir.exists() && frontendImagesDir.isDirectory() && frontendImagesDir.canWrite()) {
                // try to create subdir
                if (!frontendSubDir.exists()) {
                    boolean created = frontendSubDir.mkdirs();
                    if (!created) {
                        JsonUtil.writeError(resp, 500, "无法创建上传目录: " + frontendSubDir.getAbsolutePath());
                        return;
                    }
                }
                outFile = new File(frontendSubDir, fileName);
            } else {
                // fallback to uploads folder in working directory
                String workingDir = new File(".").getCanonicalPath();
                File uploadDir = new File(workingDir, UPLOAD_ROOT_DIR);
                if (!uploadDir.exists()) {
                    boolean ok = uploadDir.mkdirs();
                    if (!ok) {
                        JsonUtil.writeError(resp, 500, "无法创建上传目录");
                        return;
                    }
                }
                // create folder inside uploads
                File uploadSub = new File(uploadDir, subdir);
                if (!uploadSub.exists()) {
                    boolean created = uploadSub.mkdirs();
                    if (!created) {
                        JsonUtil.writeError(resp, 500, "无法创建上传目录: " + uploadSub.getAbsolutePath());
                        return;
                    }
                }
                outFile = new File(uploadSub, fileName);
            }

            try (InputStream in = filePart.getInputStream(); FileOutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }

            // Return a frontend-usable URL: use relative /images/<subdir>/<fileName> so Vite (dev) serves it on :3000
            String publicPath = "/images/" + subdir + "/" + fileName;

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("url", publicPath);
            result.put("savedPath", outFile.getAbsolutePath());
            JsonUtil.writeSuccess(resp, result);
        } catch (Exception e) {
            System.err.println("UploadServlet error: " + e.getMessage());
            JsonUtil.writeError(resp, 500, "上传失败: " + e.getMessage());
        }
    }
}
