package com.fenglin.springboottest.common;

import com.fenglin.springboottest.entity.DrAdmin;

/**
 * 当前后台管理员上下文（ThreadLocal）。
 * 由 AdminInterceptor 在鉴权通过后写入、请求结束时清理。
 *
 * @author fenglin
 */
public final class CurrentAdmin {

    private static final ThreadLocal<DrAdmin> HOLDER = new ThreadLocal<>();

    private CurrentAdmin() {
    }

    public static void set(DrAdmin admin) {
        HOLDER.set(admin);
    }

    public static DrAdmin get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
