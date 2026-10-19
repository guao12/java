package com.fenglin.springboottest.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 手写分页结果（项目未引入 MyBatis-Plus 分页插件，这里自己算 pages）。
 *
 * @param <T> 列表元素类型
 * @author fenglin
 */
public class PageVO<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> list;

    /** 总条数 */
    private Long total;

    /** 当前页码，从 1 开始 */
    private Long page;

    /** 每页条数 */
    private Long size;

    /** 总页数 */
    private Long pages;

    public PageVO() {
    }

    public PageVO(List<T> list, Long total, Long page, Long size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = size == null || size <= 0 ? 0L : (total + size - 1) / size;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getPages() {
        return pages;
    }

    public void setPages(Long pages) {
        this.pages = pages;
    }
}
