package com.fenglin.springboottest.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 检索结果：分页列表 + 分面统计 + 当前生效的筛选条件回显。
 *
 * @author fenglin
 */
public class SearchResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分页档案列表 */
    private PageVO<ArchiveVO> page;

    /** 一级分类分面（含命中条数） */
    private List<FacetVO> topCategoryFacets;

    /** 二级分类分面（含命中条数，口径与一级分面一致） */
    private List<FacetVO> categoryFacets;

    /** 密级分面（含命中条数） */
    private List<FacetVO> levelFacets;

    /** 生效条件回显 */
    private Query echo;

    /** 命中的总条数（与 page.total 相同，方便前端直接使用） */
    private Long total;

    public SearchResultVO() {
    }

    public PageVO<ArchiveVO> getPage() {
        return page;
    }

    public void setPage(PageVO<ArchiveVO> page) {
        this.page = page;
    }

    public List<FacetVO> getTopCategoryFacets() {
        return topCategoryFacets;
    }

    public void setTopCategoryFacets(List<FacetVO> topCategoryFacets) {
        this.topCategoryFacets = topCategoryFacets;
    }

    public List<FacetVO> getCategoryFacets() {
        return categoryFacets;
    }

    public void setCategoryFacets(List<FacetVO> categoryFacets) {
        this.categoryFacets = categoryFacets;
    }

    public List<FacetVO> getLevelFacets() {
        return levelFacets;
    }

    public void setLevelFacets(List<FacetVO> levelFacets) {
        this.levelFacets = levelFacets;
    }

    public Query getEcho() {
        return echo;
    }

    public void setEcho(Query echo) {
        this.echo = echo;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    /** 生效条件回显 */
    public static class Query implements Serializable {

        private static final long serialVersionUID = 1L;

        private String keyword;

        private Long topCategory;

        private Long category;

        private Integer level;

        private String sort;

        public Query() {
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
    }
}
