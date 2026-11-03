package com.bracelet.controller;

import com.bracelet.config.UserContext;
import com.bracelet.dto.AdminPasswordDTO;
import com.bracelet.dto.AdminUserCreateDTO;
import com.bracelet.dto.AdminUserUpdateDTO;
import com.bracelet.service.UserService;
import com.bracelet.util.Result;
import com.bracelet.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端用户管理 Controller — 增删改查（/admin/** 由 AdminInterceptor 校验管理员身份）
 */
@Slf4j
@RestController
@RequestMapping("/api/bracelet/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    /** 用户列表（keyword 模糊搜索用户名/昵称） */
    @GetMapping
    public Result<List<AdminUserVO>> list(@RequestParam(required = false) String keyword) {
        try {
            return Result.ok(userService.listUsers(keyword));
        } catch (Exception e) {
            log.error("查询用户列表异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    /** 新增用户 */
    @PostMapping
    public Result<AdminUserVO> create(@RequestBody AdminUserCreateDTO dto) {
        try {
            return Result.ok("用户创建成功", userService.createUserByAdmin(dto));
        } catch (RuntimeException e) {
            log.warn("管理员新增用户失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("管理员新增用户异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    /** 编辑用户（昵称/角色） */
    @PutMapping("/{id}")
    public Result<AdminUserVO> update(@PathVariable Long id, @RequestBody AdminUserUpdateDTO dto) {
        try {
            return Result.ok("用户信息已更新",
                    userService.updateUserByAdmin(UserContext.get(), id, dto));
        } catch (RuntimeException e) {
            log.warn("管理员编辑用户失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("管理员编辑用户异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    /** 重置密码 */
    @PutMapping("/{id}/password")
    public Result<String> resetPassword(@PathVariable Long id, @RequestBody AdminPasswordDTO dto) {
        try {
            userService.resetPasswordByAdmin(id, dto.getPassword());
            return Result.ok("密码已重置", null);
        } catch (RuntimeException e) {
            log.warn("管理员重置密码失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("管理员重置密码异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    /** 删除用户（逻辑删除；删除他人不可恢复展示上即"注销"） */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        try {
            userService.deleteUserByAdmin(UserContext.get(), id);
            return Result.ok("用户已删除", null);
        } catch (RuntimeException e) {
            log.warn("管理员删除用户失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("管理员删除用户异常", e);
            return Result.fail("服务器内部错误");
        }
    }
}
