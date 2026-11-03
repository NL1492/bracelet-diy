package com.bracelet.service;

import com.bracelet.dto.AddressSaveDTO;
import com.bracelet.vo.AddressVO;

import java.util.List;

/**
 * 收货地址 Service —— 地址簿 CRUD + 默认地址切换（按用户隔离）
 */
public interface AddressService {

    /** 我的地址列表（默认地址排在最前） */
    List<AddressVO> listMyAddresses(Long userId);

    /** 新增地址（首个地址自动设为默认） */
    AddressVO create(Long userId, AddressSaveDTO dto);

    /** 编辑地址（仅限本人） */
    AddressVO update(Long userId, Long id, AddressSaveDTO dto);

    /** 删除地址（逻辑删除；删掉默认地址后自动顺延一个默认） */
    void delete(Long userId, Long id);

    /** 设为默认地址 */
    void setDefault(Long userId, Long id);
}
