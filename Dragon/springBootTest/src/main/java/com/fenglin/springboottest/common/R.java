package com.fenglin.springboottest.common;

import java.io.Serializable;

/**
 * 统一响应体。前端 DragonRaja-Vue（src/api/http.js）的判定规则：
 * {@code code == 0} 成功；{@code code == 401} 登录态失效；其余数值为业务/服务器错误。
 *
 * @param <T> 业务数据类型
 * @author fenglin
 */
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 0 = 成功，400 = 参数/业务错误，401 = 未登录/令牌失效，500 = 服务器异常 */
    private Integer code;

    /** 提示信息，成功时为 "ok" */
    private String message;

    /** 业务数据 */
    private T data;

    public R() {
    }

    public R(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> ok() {
        return new R<>(0, "ok", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(0, "ok", data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(0, message, data);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(400, message, null);
    }

    public static <T> R<T> fail(Integer code, String message) {
        return new R<>(code, message, null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
