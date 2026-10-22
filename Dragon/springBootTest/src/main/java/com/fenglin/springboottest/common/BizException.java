package com.fenglin.springboottest.common;

/**
 * 业务异常。Service 层校验不通过时抛出，由 {@link GlobalExceptionHandler} 统一转成 JSON。
 *
 * @author fenglin
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = 400;
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
