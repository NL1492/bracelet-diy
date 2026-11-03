package com.bracelet.config;

import com.bracelet.entity.BraceletAdminLog;
import com.bracelet.entity.BraceletUser;
import com.bracelet.mapper.BraceletAdminLogMapper;
import com.bracelet.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 管理员操作审计拦截器 —— 在 AdminInterceptor 之后执行
 * <p>只记录写操作（POST / PUT / DELETE / PATCH），GET 查询不入库，避免日志膨胀
 * <p>日志写入失败不影响业务请求
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuditInterceptor implements HandlerInterceptor {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    private final BraceletAdminLogMapper logMapper;
    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!WRITE_METHODS.contains(request.getMethod().toUpperCase())) {
            return true;
        }
        Long userId = UserContext.get();
        if (userId == null) {
            return true;
        }
        try {
            BraceletAdminLog entry = new BraceletAdminLog();
            entry.setUserId(userId);
            BraceletUser user = userService.getById(userId);
            entry.setUsername(user == null ? null : user.getUsername());
            entry.setMethod(request.getMethod().toUpperCase());
            entry.setUri(truncate(request.getRequestURI(), 200));
            entry.setIp(clientIp(request));
            logMapper.insert(entry);
        } catch (Exception e) {
            log.warn("写入管理员操作日志失败: {}", e.getMessage());
        }
        return true;
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        String ip = (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
        return truncate(ip, 64);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
