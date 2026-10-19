package com.fenglin.springboottest.common;

import com.fenglin.springboottest.entity.DrUser;

/**
 * 档案密级（分级）工具。
 *
 * <p>密级定义与 dr_archive.security_level 一致：
 * 1=公开 2=内部 3=机密 4=绝密。</p>
 *
 * <p>可见上限由当前登录者的在校身份决定，规则与「档案借阅权限条例」卷宗一致：</p>
 * <pre>
 *   未登录 / 访客 guest ......... 1 公开
 *   新生 freshman / 在校生 student  2 内部
 *   执行部 executor ............. 3 机密
 *   教员 · 研究员 teacher ....... 4 绝密
 * </pre>
 *
 * @author fenglin
 */
public final class SecurityLevel {

    /** 公开 */
    public static final int PUBLIC = 1;

    /** 内部 */
    public static final int INTERNAL = 2;

    /** 机密 */
    public static final int SECRET = 3;

    /** 绝密 */
    public static final int TOP_SECRET = 4;

    private SecurityLevel() {
    }

    /** 密级数值 → 名称 */
    public static String label(Integer level) {
        if (level == null) {
            return "未定级";
        }
        switch (level) {
            case PUBLIC:
                return "公开";
            case INTERNAL:
                return "内部";
            case SECRET:
                return "机密";
            case TOP_SECRET:
                return "绝密";
            default:
                return "未定级";
        }
    }

    /** 归一化：把非法数值收敛到 [1,4] */
    public static int normalize(Integer level) {
        if (level == null || level < PUBLIC) {
            return PUBLIC;
        }
        return Math.min(level, TOP_SECRET);
    }

    /** 按在校身份 code 计算可见密级上限 */
    public static int maxLevelOf(String identity) {
        if (identity == null) {
            return PUBLIC;
        }
        switch (identity) {
            case "teacher":
                return TOP_SECRET;
            case "executor":
                return SECRET;
            case "freshman":
            case "student":
                return INTERNAL;
            case "guest":
            default:
                return PUBLIC;
        }
    }

    /** 当前登录者的可见密级上限；未登录为公开级 */
    public static int maxLevelOfCurrentUser() {
        DrUser user = CurrentUser.get();
        if (user == null) {
            return PUBLIC;
        }
        // 被封印的账号一律降到公开级
        if (user.getStatus() == null || user.getStatus() == 0) {
            return PUBLIC;
        }
        return maxLevelOf(user.getIdentity());
    }
}
