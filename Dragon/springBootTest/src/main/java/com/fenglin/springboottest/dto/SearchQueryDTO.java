package com.fenglin.springboottest.dto;

import java.io.Serializable;

/**
 * 档案检索请求参数。
 *
 * <p>全部为可选：不带任何参数即为「浏览全部可见档案」。</p>
 *
 * @author fenglin
 */
public class SearchQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关键词，匹配标题 / 摘要 / 标签 / 卷宗编号 */
    private String keyword;

    /** 一级分类 ID */
    private Long topCategory;

    /** 二级分类 ID */
    private Long category;

    /** 密级：1-4 */
    private Integer level;

    /** 排序：heat（热度，默认） / new（最新） / view（浏览） / level（密级） */
    private String sort;

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页条数，1-50 */
    private Integer size = 12;

    public SearchQueryDTO() {
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getTopCategory() {
        return topCategory;
    }

    public void setTopCategory(Long topCategory) {
        this.topCategory = topCategory;
    }

    public Long getCategory() {
        return category;
    }

    public void setCategory(Long category) {
        this.category = category;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
