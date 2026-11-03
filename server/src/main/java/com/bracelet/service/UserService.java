package com.bracelet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bracelet.dto.AdminUserCreateDTO;
import com.bracelet.dto.AdminUserUpdateDTO;
import com.bracelet.dto.LoginDTO;
import com.bracelet.dto.RegisterDTO;
import com.bracelet.entity.BraceletUser;
import com.bracelet.vo.AdminUserVO;
import com.bracelet.vo.LoginVO;

import java.util.List;

/**
 * 用户 Service
 */
public interface UserService extends IService<BraceletUser> {

    /** 注册（成功后直接返回登录态） */
    LoginVO register(RegisterDTO dto);

    /** 登录 */
    LoginVO login(LoginDTO dto);

    /** 查询当前用户信息 */
    LoginVO getUserInfo(Long userId);

    // ==================== 管理端用户管理 ====================

    /** 用户列表（keyword 模糊匹配用户名/昵称，仅未注销） */
    List<AdminUserVO> listUsers(String keyword);

    /** 新增用户（管理员创建） */
    AdminUserVO createUserByAdmin(AdminUserCreateDTO dto);

    /** 编辑用户（昵称/角色） */
    AdminUserVO updateUserByAdmin(Long operatorId, Long userId, AdminUserUpdateDTO dto);

    /** 重置密码 */
    void resetPasswordByAdmin(Long userId, String newPassword);

    /** 删除用户（逻辑删除；不可删自己和其他管理员） */
    void deleteUserByAdmin(Long operatorId, Long userId);
}
