package com.bracelet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bracelet.dto.AddressSaveDTO;
import com.bracelet.entity.BraceletAddress;
import com.bracelet.mapper.BraceletAddressMapper;
import com.bracelet.service.AddressService;
import com.bracelet.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 收货地址 Service 实现
 * <p>同一用户仅允许一个默认地址；地址归属校验以 user_id 隔离
 */
@Service
@RequiredArgsConstructor
public class AddressServiceImpl
        extends ServiceImpl<BraceletAddressMapper, BraceletAddress>
        implements AddressService {

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");

    @Override
    public List<AddressVO> listMyAddresses(Long userId) {
        return lambdaQuery()
                .eq(BraceletAddress::getUserId, userId)
                .orderByDesc(BraceletAddress::getIsDefault)
                .orderByDesc(BraceletAddress::getCreateTime)
                .list()
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO create(Long userId, AddressSaveDTO dto) {
        validate(dto);
        boolean first = lambdaQuery().eq(BraceletAddress::getUserId, userId).count() == 0;

        BraceletAddress a = new BraceletAddress();
        a.setUserId(userId);
        a.setIsEnable(1);
        copy(a, dto);
        a.setIsDefault(first || isTrue(dto.getIsDefault()) ? 1 : 0);
        save(a);

        if (a.getIsDefault() == 1) {
            clearOtherDefaults(userId, a.getId());
        }
        return toVO(a);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO update(Long userId, Long id, AddressSaveDTO dto) {
        validate(dto);
        BraceletAddress a = requireOwned(userId, id);
        copy(a, dto);
        // 已是默认地址时不允许被编辑成非默认，避免出现“无默认地址”
        if (isTrue(dto.getIsDefault()) || (a.getIsDefault() != null && a.getIsDefault() == 1)) {
            a.setIsDefault(1);
        } else {
            a.setIsDefault(0);
        }
        updateById(a);

        if (a.getIsDefault() == 1) {
            clearOtherDefaults(userId, a.getId());
        }
        return toVO(a);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        boolean wasDefault = requireOwned(userId, id).getIsDefault() == 1;
        removeById(id);
        if (wasDefault) {
            // 默认地址被删 → 顺延最近创建的一条为默认
            BraceletAddress next = lambdaQuery()
                    .eq(BraceletAddress::getUserId, userId)
                    .orderByDesc(BraceletAddress::getCreateTime)
                    .last("LIMIT 1")
                    .one();
            if (next != null) {
                lambdaUpdate()
                        .eq(BraceletAddress::getId, next.getId())
                        .set(BraceletAddress::getIsDefault, 1)
                        .update();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long id) {
        requireOwned(userId, id);
        clearOtherDefaults(userId, id);
        lambdaUpdate()
                .eq(BraceletAddress::getId, id)
                .set(BraceletAddress::getIsDefault, 1)
                .update();
    }

    // ==================== 内部方法 ====================

    /** 归属校验：地址必须属于当前用户且未被删除 */
    private BraceletAddress requireOwned(Long userId, Long id) {
        BraceletAddress a = getById(id);
        if (a == null || a.getIsEnable() == null || a.getIsEnable() != 1) {
            throw new RuntimeException("收货地址不存在");
        }
        if (!userId.equals(a.getUserId())) {
            throw new RuntimeException("无权操作该收货地址");
        }
        return a;
    }

    /** 清除同用户其他地址的默认标记 */
    private void clearOtherDefaults(Long userId, Long keepId) {
        lambdaUpdate()
                .eq(BraceletAddress::getUserId, userId)
                .ne(BraceletAddress::getId, keepId)
                .eq(BraceletAddress::getIsDefault, 1)
                .set(BraceletAddress::getIsDefault, 0)
                .update();
    }

    private void copy(BraceletAddress a, AddressSaveDTO dto) {
        a.setReceiverName(dto.getReceiverName().trim());
        a.setReceiverPhone(dto.getReceiverPhone().trim());
        a.setRegion(dto.getRegion().trim());
        a.setDetail(dto.getDetail().trim());
    }

    private void validate(AddressSaveDTO dto) {
        if (dto == null) {
            throw new RuntimeException("请填写收货地址");
        }
        if (isBlank(dto.getReceiverName())) {
            throw new RuntimeException("请填写收货人");
        }
        if (isBlank(dto.getReceiverPhone())) {
            throw new RuntimeException("请填写联系电话");
        }
        if (!PHONE.matcher(dto.getReceiverPhone().trim()).matches()) {
            throw new RuntimeException("手机号需为 11 位，且以 1 开头");
        }
        if (isBlank(dto.getRegion())) {
            throw new RuntimeException("请填写所在地区");
        }
        if (isBlank(dto.getDetail())) {
            throw new RuntimeException("请填写详细地址");
        }
    }

    private AddressVO toVO(BraceletAddress a) {
        return AddressVO.builder()
                .id(a.getId())
                .receiverName(a.getReceiverName())
                .receiverPhone(a.getReceiverPhone())
                .region(a.getRegion())
                .detail(a.getDetail())
                .isDefault(a.getIsDefault())
                .build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean isTrue(Integer v) {
        return v != null && v == 1;
    }
}
