package com.bracelet.config;

import com.bracelet.entity.BraceletUser;
import com.bracelet.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员权限拦截器 — 在 AuthInterceptor 之后执行
 * <p>校验当前登录用户 role=admin，否则返回业务码 403
 */
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        Long userId = UserContext.get();
        if (userId != null) {
            BraceletUser user = userService.getById(userId);
            if (user != null && user.getIsEnable() != null && user.getIsEnable() == 1
                    && "admin".equals(user.getRole())) {
                return true;
            }
        }

        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"msg\":\"无管理员权限\",\"data\":null}");
        return false;
    }
}
