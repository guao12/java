package com.fenglin.springboottest.vo;

import java.io.Serializable;

/**
 * 收藏结果：前端据此切换按钮状态，不必再拉一次详情。
 *
 * @author fenglin
 */
public class CollectVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 本次操作后是否处于「已收藏」状态 */
    private boolean collected;

    public CollectVO() {
    }

    public CollectVO(boolean collected) {
        this.collected = collected;
    }

    public boolean isCollected() {
        return collected;
    }

    public void setCollected(boolean collected) {
        this.collected = collected;
    }
}
