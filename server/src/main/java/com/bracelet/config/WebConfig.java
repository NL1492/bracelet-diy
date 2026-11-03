package com.bracelet.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 — 注册登录拦截器
 * <p>公开接口：登录 / 注册 / 珠子列表 / 实时估价（灵感详情未登录可浏览）
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminInterceptor adminInterceptor;
    private final AdminAuditInterceptor adminAuditInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 登录鉴权（解析 token → UserContext）
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/bracelet/user/login",
                        "/api/bracelet/user/register",
                        "/api/bracelet/beads",
                        "/api/bracelet/getCountData",
                        "/api/bracelet/inspirations",
                        "/api/bracelet/inspirations/*/use"
                );
        // 2. 管理员权限（在登录之后执行，校验 role=admin）
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/bracelet/admin/**");
        // 3. 管理员操作审计（在权限校验之后执行，只记录写操作）
        registry.addInterceptor(adminAuditInterceptor)
                .addPathPatterns("/api/bracelet/admin/**");
    }
}
