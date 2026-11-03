package com.bracelet.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回结果工具类
 * <p>格式: { "code": 200, "msg": "success", "data": {...} }
 *
 * @param <T> data 泛型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 业务状态码 */
    private int code;
    /** 提示信息 */
    private String msg;
    /** 返回数据 */
    private T data;

    // ==================== 静态工厂方法 ====================

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> fail(int code, String msg, T data) {
        return new Result<>(code, msg, data);
    }
}
