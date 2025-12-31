package org.example.examback.entity;

import lombok.Data;
/*
 * @Author:总会落叶
 * @Date:2025/12/29
 * @Description:
 */
@Data
public class RestBean<T> {
    private int code;           // 状态码
    private boolean success;    // 是否成功
    private String type;        // 类型：success/info/warning/error
    private String message;     // 提示消息
    private T data;             // 数据

    public RestBean(int code, boolean success, String type, String message, T data) {
        this.code = code;
        this.success = success;
        this.type = type;
        this.message = message;
        this.data = data;
    }

    // ========== 成功相关方法 ==========
    public static <T> RestBean<T> success() {
        return new RestBean<>(200, true, "success", "操作成功", null);
    }

    public static <T> RestBean<T> success(String message) {
        return new RestBean<>(200, true, "success", message, null);
    }

    public static <T> RestBean<T> success(T data) {
        return new RestBean<>(200, true, "success", "操作成功", data);
    }

    public static <T> RestBean<T> success(String message, T data) {
        return new RestBean<>(200, true, "success", message, data);
    }

    // ========== 信息提示方法 ==========
    public static <T> RestBean<T> info(String message) {
        return new RestBean<>(200, true, "info", message, null);
    }

    public static <T> RestBean<T> info(String message, T data) {
        return new RestBean<>(200, true, "info", message, data);
    }

    // ========== 警告提示方法 ==========
    public static <T> RestBean<T> warning(String message) {
        return new RestBean<>(400, false, "warning", message, null);
    }

    public static <T> RestBean<T> warning(int code, String message) {
        return new RestBean<>(code, false, "warning", message, null);
    }

    public static <T> RestBean<T> warning(String message, T data) {
        return new RestBean<>(400, false, "warning", message, data);
    }

    // ========== 错误提示方法 ==========
    public static <T> RestBean<T> error(String message) {
        return new RestBean<>(500, false, "error", message, null);
    }

    public static <T> RestBean<T> error(int code, String message) {
        return new RestBean<>(code, false, "error", message, null);
    }

    public static <T> RestBean<T> error(int code, String message, T data) {
        return new RestBean<>(code, false, "error", message, data);
    }

    // ========== 保留原有的 failure 方法 ==========
    public static <T> RestBean<T> failure(int code, String message) {
        return new RestBean<>(code, false, "error", message, null);
    }

    public static <T> RestBean<T> failure(String message) {
        return new RestBean<>(500, false, "error", message, null);
    }

    // ========== 便捷方法 ==========
    public boolean isError() {
        return "error".equals(this.type);
    }

    public boolean isWarning() {
        return "warning".equals(this.type);
    }

    public boolean isInfo() {
        return "info".equals(this.type);
    }

    public boolean isSuccess() {
        return "success".equals(this.type);
    }

    // ========== 兼容旧代码的方法 ==========
    @Deprecated
    public String getStatus() {
        return this.type;
    }

    @Deprecated
    public void setStatus(String status) {
        this.type = status;
    }
}