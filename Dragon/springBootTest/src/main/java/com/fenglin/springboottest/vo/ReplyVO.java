package com.fenglin.springboottest.vo;

import java.io.Serializable;

/**
 * 批注（楼层）视图。
 *
 * <p>{@code quote*} 三个字段是引用快照：被引用的批注即使之后被折叠或删除，
 * 这里仍然能看到当时的楼层与摘要，不至于变成一条断掉的引用。</p>
 *
 * @author fenglin
 */
public class ReplyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer floorNo;
    private Long authorId;
    private String authorName;
    private String authorIdentity;
    private String content;
    private String createdAt;

    /** 被引用批注的楼层，0 = 无引用 */
    private Integer quoteFloor;
    private String quoteAuthor;
    private String quoteText;

    /** 是否本人所发 */
    private boolean mine;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(Integer floorNo) {
        this.floorNo = floorNo;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorIdentity() {
        return authorIdentity;
    }

    public void setAuthorIdentity(String authorIdentity) {
        this.authorIdentity = authorIdentity;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getQuoteFloor() {
        return quoteFloor;
    }

    public void setQuoteFloor(Integer quoteFloor) {
        this.quoteFloor = quoteFloor;
    }

    public String getQuoteAuthor() {
        return quoteAuthor;
    }

    public void setQuoteAuthor(String quoteAuthor) {
        this.quoteAuthor = quoteAuthor;
    }

    public String getQuoteText() {
        return quoteText;
    }

    public void setQuoteText(String quoteText) {
        this.quoteText = quoteText;
    }

    public boolean isMine() {
        return mine;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }
}
