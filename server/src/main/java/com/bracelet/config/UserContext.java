package com.bracelet.config;

/**
 * 当前登录用户上下文 — 基于 ThreadLocal
 * <p>AuthInterceptor 在请求进入时写入，afterCompletion 时清理
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    /** 获取当前用户ID，未登录返回 null */
    public static Long get() {
        return USER_ID.get();
    }

    /** 获取当前用户ID，未登录抛业务异常 */
    public static Long require() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new RuntimeException("请先登录");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
