package com.fenglin.springboottest.vo;

import java.io.Serializable;

/**
 * 点赞结果：前端据此切换按钮状态，不需要再拉一次详情。
 *
 * @author fenglin
 */
public class LikeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 本次操作后是否处于「已赞」状态 */
    private boolean liked;

    /** 最新点赞总数 */
    private Integer count;

    public LikeVO() {
    }

    public LikeVO(boolean liked, Integer count) {
        this.liked = liked;
        this.count = count;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
