package com.fenglin.springboottest.common;

import com.fenglin.springboottest.entity.DrUser;

/**
 * 当前登录用户上下文（ThreadLocal）。
 * 由 JwtInterceptor 在鉴权通过后写入、请求结束时清理。
 *
 * @author fenglin
 */
public final class CurrentUser {

    private static final ThreadLocal<DrUser> HOLDER = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void set(DrUser user) {
        HOLDER.set(user);
    }

    public static DrUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
