package com.bracelet.config;

import com.bracelet.entity.BraceletUser;
import com.bracelet.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 管理员账号初始化 — 应用启动时自动创建默认管理员（若不存在）
 * <p>账号：admin / admin123（BCrypt 加密存储，请尽快修改默认密码）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        try {
            if (userService.lambdaQuery().eq(BraceletUser::getUsername, "admin").count() > 0) {
                return;
            }
            BraceletUser admin = new BraceletUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("管理员");
            admin.setRole("admin");
            userService.save(admin);
            log.info("已初始化管理员账号: admin / admin123（请尽快修改默认密码）");
        } catch (Exception e) {
            log.error("管理员账号初始化失败", e);
        }
    }
}
