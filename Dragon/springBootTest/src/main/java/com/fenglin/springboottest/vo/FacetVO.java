package com.fenglin.springboottest.vo;

import java.io.Serializable;

/**
 * 检索分面统计项：在「当前已生效的其他筛选条件」下，某个维度的命中条数。
 *
 * <p>例如选中一级分类「人物志」后，密级分面显示的就是人物志下各密级的条数，
 * 前端据此渲染可点击的筛选项与角标数字。</p>
 *
 * @author fenglin
 */
public class FacetVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 维度值：分类 ID，或密级数值 */
    private Long key;

    /** 维度显示名：分类名，或「公开 / 内部 / 机密 / 绝密」 */
    private String label;

    /** 命中条数 */
    private Long total;

    public FacetVO() {
    }

    public FacetVO(Long key, String label, Long total) {
        this.key = key;
        this.label = label;
        this.total = total;
    }

    public Long getKey() {
        return key;
    }

    public void setKey(Long key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
