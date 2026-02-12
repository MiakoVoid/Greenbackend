package com.it.utils;

import lombok.Data;

@Data
public class Result {
    private boolean success;
    private int code;
    private String message;
    private Object data;
    
    // 成功响应（指定消息和数据）
    public static Result ok(String message, Object data) {
        Result result = new Result();
        result.success = true;
        result.code = 200;
        result.message = message;
        result.data = data;
        return result;
    }
    
    // 成功响应（默认消息，指定数据）
    public static Result ok(Object data) {
        return ok("success", data);
    }
    
    // 成功响应（只有消息，无数据）
    public static Result ok(String message) {
        return ok(message, null);
    }
    
    // 成功响应（无消息无数据）
    public static Result ok() {
        return ok("success", null);
    }
    
    // 错误响应
    public static Result error(int code, String message) {
        Result result = new Result();
        result.success = false;
        result.code = code;
        result.message = message;
        result.data = null;
        return result;
    }
    
    // 错误响应（带数据）
    public static Result error(int code, String message, Object data) {
        Result result = new Result();
        result.success = false;
        result.code = code;
        result.message = message;
        result.data = data;
        return result;
    }
}
