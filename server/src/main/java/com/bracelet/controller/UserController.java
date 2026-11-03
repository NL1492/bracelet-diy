package com.bracelet.controller;

import com.bracelet.config.UserContext;
import com.bracelet.dto.LoginDTO;
import com.bracelet.dto.RegisterDTO;
import com.bracelet.service.UserService;
import com.bracelet.util.Result;
import com.bracelet.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户 Controller — 注册 / 登录 / 登录态 / 退出
 */
@Slf4j
@RestController
@RequestMapping("/api/bracelet/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 注册（成功直接返回 token，即注册即登录） */
    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody RegisterDTO dto) {
        try {
            return Result.ok("注册成功", userService.register(dto));
        } catch (RuntimeException e) {
            log.warn("注册失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("注册异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        try {
            return Result.ok("登录成功", userService.login(dto));
        } catch (RuntimeException e) {
            log.warn("登录失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("登录异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    /** 查询当前登录用户信息（前端启动时校验 token 用） */
    @GetMapping("/info")
    public Result<LoginVO> info() {
        try {
            Long userId = UserContext.get();
            if (userId == null) {
                return Result.fail(401, "请先登录");
            }
            return Result.ok(userService.getUserInfo(userId));
        } catch (RuntimeException e) {
            log.warn("查询用户信息失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询用户信息异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    /** 退出登录（JWT 无状态，服务端仅清理上下文，前端删除本地 token） */
    @PostMapping("/logout")
    public Result<String> logout() {
        UserContext.clear();
        return Result.ok("已退出登录", null);
    }
}
