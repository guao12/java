package com.fenglin.springboottest.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 主题详情视图：主楼 + 楼层。
 *
 * <p>正文是否返回由 Service 判定，三种情况：</p>
 * <ul>
 *   <li>正常：{@code content} 有值；</li>
 *   <li>密级不足：{@code contentHidden=true}，{@code hiddenReason} 说明还差什么；</li>
 *   <li>回复可见且未回复：同样隐藏，提示回复后解锁。</li>
 * </ul>
 * <p>无论哪种情况，标题与作者都返回 —— 论坛需要让人知道「有这个东西存在」，
 * 否则权限体系就没有追求的目标。</p>
 *
 * @author fenglin
 */
public class ThreadDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主题基本信息（与列表项同构，前端可复用组件） */
    private ThreadVO thread;

    /** 正文；隐藏时为空串 */
    private String content;

    /** 正文是否被隐藏 */
    private boolean contentHidden;

    /** 隐藏原因，直接展示给用户 */
    private String hiddenReason;

    /** 楼层（不含主楼） */
    private List<ReplyVO> replies = new ArrayList<>();

    /** 当前访问者能否回复 */
    private boolean canReply;

    /** 不能回复的原因 */
    private String replyHint;

    /** 「回复可见」是否已解锁 */
    private boolean replyUnlocked;

    /** 当前访问者的可见密级上限，前端用来解释权限 */
    private Integer maxLevel;

    public ThreadVO getThread() {
        return thread;
    }

    public void setThread(ThreadVO thread) {
        this.thread = thread;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isContentHidden() {
        return contentHidden;
    }

    public void setContentHidden(boolean contentHidden) {
        this.contentHidden = contentHidden;
    }

    public String getHiddenReason() {
        return hiddenReason;
    }

    public void setHiddenReason(String hiddenReason) {
        this.hiddenReason = hiddenReason;
    }

    public List<ReplyVO> getReplies() {
        return replies;
    }

    public void setReplies(List<ReplyVO> replies) {
        this.replies = replies;
    }

    public boolean isCanReply() {
        return canReply;
    }

    public void setCanReply(boolean canReply) {
        this.canReply = canReply;
    }

    public String getReplyHint() {
        return replyHint;
    }

    public void setReplyHint(String replyHint) {
        this.replyHint = replyHint;
    }

    public boolean isReplyUnlocked() {
        return replyUnlocked;
    }

    public void setReplyUnlocked(boolean replyUnlocked) {
        this.replyUnlocked = replyUnlocked;
    }

    public Integer getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(Integer maxLevel) {
        this.maxLevel = maxLevel;
    }
}
