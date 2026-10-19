package com.fenglin.springboottest.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 档案分类视图对象。
 *
 * <p>一级分类带 children（二级分类）；二级分类的 children 为空列表。
 * count 为该分类下「当前访问者可见」的档案条数。</p>
 *
 * @author fenglin
 */
public class CategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long parentId;

    private String code;

    private String name;

    private String icon;

    private Integer sortNo;

    /** 该分类下可见档案数（一级分类为整个大类合计） */
    private Long count;

    /** 二级分类列表，仅一级分类有值 */
    private List<CategoryVO> children;

    public CategoryVO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public List<CategoryVO> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryVO> children) {
        this.children = children;
    }
}
