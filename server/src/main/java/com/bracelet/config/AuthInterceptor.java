package com.bracelet.config;

import com.bracelet.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录认证拦截器 — 解析 Authorization: Bearer <token>
 * <p>解析成功写入 UserContext；失败返回 HTTP 200 + 业务码 401（与前端约定）
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 跨域预检放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 静态资源等非 Controller 放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Long userId = jwtUtil.parseUserId(token);
                UserContext.set(userId);
                request.setAttribute("userId", userId);
                return true;
            } catch (Exception ignored) {
                // token 无效/过期，走下方 401
            }
        }

        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\",\"data\":null}");
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
