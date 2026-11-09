package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 后台「主题管理」的新建 / 编辑请求。
 *
 * <p>与学员端 {@link ThreadCreateDTO} 的差别，都是刻意的：</p>
 * <ul>
 *   <li>版块用 {@code boardId} 而不是 {@code boardCode}：后台是下拉框选，
 *       给主键比让管理员手打 code 更不容易选错；</li>
 *   <li>多了 {@code pinned / essence / status} 三个版务字段 —— 管理员发公告时
 *       通常要一次到位（置顶 + 正常状态），没必要发完再回头改两遍；</li>
 *   <li>不做发帖频率限制与敏感词拦截：后台是人工审校过的内容，
 *       拿普通用户的防刷规则去卡管理员只会添堵。</li>
 * </ul>
 *
 * @author fenglin
 */
public class ThreadAdminSaveDTO {

    /** 版块主键 */
    @NotNull(message = "请选择版块")
    private Long boardId;

    @NotBlank(message = "标题不能为空")
    @Size(min = 4, max = 80, message = "标题需 4–80 字")
    private String title;

    @NotBlank(message = "正文不能为空")
    @Size(min = 10, max = 8000, message = "正文需 10–8000 字")
    private String content;

    /** 1 公开 2 内部 3 机密 4 绝密；为空按 1 处理 */
    private Integer securityLevel;

    /** 是否「回复后可见」 */
    private Boolean replyVisible;

    /** 是否置顶 */
    private Boolean pinned;

    /** 是否加精 */
    private Boolean essence;

    /** 1 正常 / 0 待审 / -1 已下线；为空按 1 处理 */
    private Integer status;

    public Long getBoardId() {
        return boardId;
    }

    public void setBoardId(Long boardId) {
        this.boardId = boardId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(Integer securityLevel) {
        this.securityLevel = securityLevel;
    }

    public Boolean getReplyVisible() {
        return replyVisible;
    }

    public void setReplyVisible(Boolean replyVisible) {
        this.replyVisible = replyVisible;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }

    public Boolean getEssence() {
        return essence;
    }

    public void setEssence(Boolean essence) {
        this.essence = essence;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
