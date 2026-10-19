package com.fenglin.springboottest.vo;

import java.io.Serializable;

/**
 * 首页公告视图对象。
 *
 * @author fenglin
 */
public class NoticeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String content;

    /** 紧急度：1=普通 2=重要 3=紧急 */
    private Integer level;

    /** 紧急度名称 */
    private String levelLabel;

    /** 是否置顶 */
    private Boolean pinned;

    private String publisher;

    /** 发布时间，yyyy-MM-dd HH:mm */
    private String publishAt;

    public NoticeVO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getLevelLabel() {
        return levelLabel;
    }

    public void setLevelLabel(String levelLabel) {
        this.levelLabel = levelLabel;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublishAt() {
        return publishAt;
    }

    public void setPublishAt(String publishAt) {
        this.publishAt = publishAt;
    }
}
