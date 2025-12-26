package com.carrental.util;

/**
 * Simple request-scoped context for logging.
 */
public final class LogContext {
    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private LogContext() {}

    public static void set(Long userId, String username, String role, String ip, String userAgent) {
        Context ctx = new Context();
        ctx.userId = userId;
        ctx.username = username;
        ctx.role = role;
        ctx.ip = ip;
        ctx.userAgent = userAgent;
        HOLDER.set(ctx);
    }

    public static Long getUserId() {
        Context ctx = HOLDER.get();
        return ctx != null ? ctx.userId : null;
    }

    public static String getUsername() {
        Context ctx = HOLDER.get();
        return ctx != null ? ctx.username : null;
    }

    public static String getRole() {
        Context ctx = HOLDER.get();
        return ctx != null ? ctx.role : null;
    }

    public static String getIp() {
        Context ctx = HOLDER.get();
        return ctx != null ? ctx.ip : null;
    }

    public static String getUserAgent() {
        Context ctx = HOLDER.get();
        return ctx != null ? ctx.userAgent : null;
    }

    public static void clear() {
        HOLDER.remove();
    }

    private static final class Context {
        private Long userId;
        private String username;
        private String role;
        private String ip;
        private String userAgent;
    }
}
