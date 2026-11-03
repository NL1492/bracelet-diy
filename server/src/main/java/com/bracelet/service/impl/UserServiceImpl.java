package com.bracelet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bracelet.config.UserContext;
import com.bracelet.dto.AdminUserCreateDTO;
import com.bracelet.dto.AdminUserUpdateDTO;
import com.bracelet.dto.LoginDTO;
import com.bracelet.dto.RegisterDTO;
import com.bracelet.entity.BraceletUser;
import com.bracelet.mapper.BraceletUserMapper;
import com.bracelet.service.UserService;
import com.bracelet.util.JwtUtil;
import com.bracelet.vo.AdminUserVO;
import com.bracelet.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<BraceletUserMapper, BraceletUser>
        implements UserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;

    @Override
    public LoginVO register(RegisterDTO dto) {
        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        String password = dto.getPassword() == null ? "" : dto.getPassword();

        if (username.length() < 3 || username.length() > 20) {
            throw new RuntimeException("用户名长度需在 3-20 位之间");
        }
        if (!username.matches("^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$")) {
            throw new RuntimeException("用户名仅支持中文、字母、数字、下划线");
        }
        if (password.length() < 6 || password.length() > 32) {
            throw new RuntimeException("密码长度需在 6-32 位之间");
        }
        if (this.lambdaQuery().eq(BraceletUser::getUsername, username).count() > 0) {
            throw new RuntimeException("用户名已被注册");
        }

        BraceletUser user = new BraceletUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        String nickname = dto.getNickname();
        user.setNickname(nickname != null && !nickname.isBlank() ? nickname.trim() : username);
        user.setRole("user");
        this.save(user);

        log.info("用户注册成功: id={}, username={}", user.getId(), username);
        return buildVO(user, jwtUtil.generateToken(user.getId(), user.getUsername()));
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        String password = dto.getPassword() == null ? "" : dto.getPassword();

        BraceletUser user = this.lambdaQuery()
                .eq(BraceletUser::getUsername, username)
                .eq(BraceletUser::getIsEnable, 1)
                .one();
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        log.info("用户登录成功: id={}, username={}", user.getId(), username);
        return buildVO(user, jwtUtil.generateToken(user.getId(), user.getUsername()));
    }

    @Override
    public LoginVO getUserInfo(Long userId) {
        BraceletUser user = this.getById(userId);
        if (user == null || user.getIsEnable() == null || user.getIsEnable() != 1) {
            throw new RuntimeException("用户不存在或已注销");
        }
        return buildVO(user, null);
    }

    // ==================== 管理端用户管理 ====================

    @Override
    public List<AdminUserVO> listUsers(String keyword) {
        LambdaQueryWrapper<BraceletUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BraceletUser::getIsEnable, 1);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(BraceletUser::getUsername, kw)
                    .or().like(BraceletUser::getNickname, kw));
        }
        wrapper.orderByAsc(BraceletUser::getId);
        return this.list(wrapper).stream().map(this::toAdminVO).collect(Collectors.toList());
    }

    @Override
    public AdminUserVO createUserByAdmin(AdminUserCreateDTO dto) {
        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        String password = dto.getPassword() == null ? "" : dto.getPassword();

        if (username.length() < 3 || username.length() > 20) {
            throw new RuntimeException("用户名长度需在 3-20 位之间");
        }
        if (!username.matches("^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$")) {
            throw new RuntimeException("用户名仅支持中文、字母、数字、下划线");
        }
        if (password.length() < 6 || password.length() > 32) {
            throw new RuntimeException("密码长度需在 6-32 位之间");
        }
        String role = "admin".equals(dto.getRole()) ? "admin" : "user";
        // 用户名唯一索引包含已注销账号，故不过滤 isEnable，避免撞唯一键
        if (this.lambdaQuery().eq(BraceletUser::getUsername, username).count() > 0) {
            throw new RuntimeException("用户名已被注册");
        }

        BraceletUser user = new BraceletUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(dto.getNickname() != null && !dto.getNickname().isBlank()
                ? dto.getNickname().trim() : username);
        user.setRole(role);
        this.save(user);

        log.info("管理员新增用户: id={}, username={}, role={}, operatorId={}",
                user.getId(), username, role, UserContext.get());
        return toAdminVO(user);
    }

    @Override
    public AdminUserVO updateUserByAdmin(Long operatorId, Long userId, AdminUserUpdateDTO dto) {
        BraceletUser user = getEnabledUser(userId);
        if (operatorId != null && operatorId.equals(userId)
                && dto.getRole() != null && !"admin".equals(dto.getRole())) {
            throw new RuntimeException("不能修改自己的角色");
        }
        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            user.setNickname(dto.getNickname().trim());
        }
        if (dto.getRole() != null) {
            if (!"user".equals(dto.getRole()) && !"admin".equals(dto.getRole())) {
                throw new RuntimeException("角色仅支持 user / admin");
            }
            user.setRole(dto.getRole());
        }
        this.updateById(user);
        log.info("管理员编辑用户: id={}, operatorId={}", userId, operatorId);
        return toAdminVO(user);
    }

    @Override
    public void resetPasswordByAdmin(Long userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 32) {
            throw new RuntimeException("密码长度需在 6-32 位之间");
        }
        BraceletUser user = getEnabledUser(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
        log.info("管理员重置用户密码: id={}, operatorId={}", userId, UserContext.get());
    }

    @Override
    public void deleteUserByAdmin(Long operatorId, Long userId) {
        BraceletUser user = getEnabledUser(userId);
        if (operatorId != null && operatorId.equals(userId)) {
            throw new RuntimeException("不能删除自己的账号");
        }
        if ("admin".equals(user.getRole())) {
            throw new RuntimeException("管理员账号不允许删除");
        }
        user.setIsEnable(0);
        // 释放用户名：追加注销后缀腾出唯一索引位，原用户名可被重新注册；保留原名便于数据追溯
        user.setUsername(user.getUsername() + "_del_" + userId);
        this.updateById(user);
        log.info("管理员删除用户(逻辑删除): id={}, username={}, operatorId={}",
                userId, user.getUsername(), operatorId);
    }

    private BraceletUser getEnabledUser(Long userId) {
        BraceletUser user = this.getById(userId);
        if (user == null || user.getIsEnable() == null || user.getIsEnable() != 1) {
            throw new RuntimeException("用户不存在或已注销");
        }
        return user;
    }

    private AdminUserVO toAdminVO(BraceletUser user) {
        return AdminUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname() != null ? user.getNickname() : user.getUsername())
                .role(user.getRole() != null ? user.getRole() : "user")
                .isEnable(user.getIsEnable())
                .createTime(user.getCreateTime())
                .build();
    }

    private LoginVO buildVO(BraceletUser user, String token) {
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname() != null ? user.getNickname() : user.getUsername())
                .avatar(user.getAvatar())
                .role(user.getRole() != null ? user.getRole() : "user")
                .build();
    }
}
