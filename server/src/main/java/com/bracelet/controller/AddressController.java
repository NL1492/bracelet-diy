package com.bracelet.controller;

import com.bracelet.config.UserContext;
import com.bracelet.dto.AddressSaveDTO;
import com.bracelet.service.AddressService;
import com.bracelet.util.Result;
import com.bracelet.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C 端收货地址簿 Controller —— 列表 / 新增 / 编辑 / 删除 / 设默认
 */
@Slf4j
@RestController
@RequestMapping("/api/bracelet/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public Result<List<AddressVO>> list() {
        try {
            return Result.ok(addressService.listMyAddresses(UserContext.require()));
        } catch (RuntimeException e) {
            log.warn("查询收货地址失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询收货地址异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    @PostMapping
    public Result<AddressVO> create(@RequestBody AddressSaveDTO dto) {
        try {
            return Result.ok("已保存", addressService.create(UserContext.require(), dto));
        } catch (RuntimeException e) {
            log.warn("新增收货地址失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("新增收货地址异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    @PutMapping("/{id}")
    public Result<AddressVO> update(@PathVariable Long id, @RequestBody AddressSaveDTO dto) {
        try {
            return Result.ok("已更新", addressService.update(UserContext.require(), id, dto));
        } catch (RuntimeException e) {
            log.warn("编辑收货地址失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("编辑收货地址异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        try {
            addressService.delete(UserContext.require(), id);
            return Result.ok("已删除", null);
        } catch (RuntimeException e) {
            log.warn("删除收货地址失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("删除收货地址异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    @PutMapping("/{id}/default")
    public Result<String> setDefault(@PathVariable Long id) {
        try {
            addressService.setDefault(UserContext.require(), id);
            return Result.ok("已设为默认地址", null);
        } catch (RuntimeException e) {
            log.warn("设置默认地址失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("设置默认地址异常", e);
            return Result.fail("服务器内部错误");
        }
    }
}
