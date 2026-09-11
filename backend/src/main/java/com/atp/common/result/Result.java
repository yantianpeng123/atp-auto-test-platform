package com.atp.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {

    private Integer code;
    private String message;
    private T data;
    //去掉timestamp属性
    private Long timestamp;

    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(ResultCode.SUCCESS.getMessage());
        return r;
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = success();
        r.setData(data);
        return r;
    }

    public static <T> Result<T> ok(String message) {
        Result<T> r = success();
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> r = success(data);
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> failure(ResultCode resultCode) {
        Result<T> r = new Result<>();
        r.setCode(resultCode.getCode());
        r.setMessage(resultCode.getMessage());
        return r;
    }

    public static <T> Result<T> failure(ResultCode resultCode, String message) {
        Result<T> r = failure(resultCode);
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> failure(Integer code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
}
