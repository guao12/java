package com.fenglin.springboottest.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 首页概览：一次请求拿齐首页需要的全部数据，避免前端并发多个接口。
 *
 * @author fenglin
 */
public class HomeOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前访问者的档案权限信息 */
    private Access access;

    /** 统计面板 */
    private Stats stats;

    /** 学院通报（置顶优先） */
    private List<NoticeVO> notices;

    /** 两级分类树（带可见档案数） */
    private List<CategoryVO> categories;

    /** 精选卷宗 */
    private List<ArchiveVO> spotlight;

    /** 热门卷宗 */
    private List<ArchiveVO> hot;

    /** 最新归档 */
    private List<ArchiveVO> latest;

    /** 热门检索词 */
    private List<String> hotKeywords;

    public HomeOverviewVO() {
    }

    public Access getAccess() {
        return access;
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public List<NoticeVO> getNotices() {
        return notices;
    }

    public void setNotices(List<NoticeVO> notices) {
        this.notices = notices;
    }

    public List<CategoryVO> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryVO> categories) {
        this.categories = categories;
    }

    public List<ArchiveVO> getSpotlight() {
        return spotlight;
    }

    public void setSpotlight(List<ArchiveVO> spotlight) {
        this.spotlight = spotlight;
    }

    public List<ArchiveVO> getHot() {
        return hot;
    }

    public void setHot(List<ArchiveVO> hot) {
        this.hot = hot;
    }

    public List<ArchiveVO> getLatest() {
        return latest;
    }

    public void setLatest(List<ArchiveVO> latest) {
        this.latest = latest;
    }

    public List<String> getHotKeywords() {
        return hotKeywords;
    }

    public void setHotKeywords(List<String> hotKeywords) {
        this.hotKeywords = hotKeywords;
    }

    /** 当前访问者的档案权限 */
    public static class Access implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 是否已登录 */
        private Boolean logged;

        /** 姓名 / 行动代号，未登录为空 */
        private String codename;

        /** 在校身份名称 */
        private String identityName;

        /** 可见密级上限：1-4 */
        private Integer maxLevel;

        /** 可见密级名称 */
        private String maxLevelLabel;

        public Access() {
        }

        public Boolean getLogged() {
            return logged;
        }

        public void setLogged(Boolean logged) {
            this.logged = logged;
        }

        public String getCodename() {
            return codename;
        }

        public void setCodename(String codename) {
            this.codename = codename;
        }

        public String getIdentityName() {
            return identityName;
        }

        public void setIdentityName(String identityName) {
            this.identityName = identityName;
        }

        public Integer getMaxLevel() {
            return maxLevel;
        }

        public void setMaxLevel(Integer maxLevel) {
            this.maxLevel = maxLevel;
        }

        public String getMaxLevelLabel() {
            return maxLevelLabel;
        }

        public void setMaxLevelLabel(String maxLevelLabel) {
            this.maxLevelLabel = maxLevelLabel;
        }
    }

    /** 统计面板 */
    public static class Stats implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 可见档案总数 */
        private Long archiveCount;

        /** 分类总数 */
        private Long categoryCount;

        /** 其中可查阅的绝密卷宗数 */
        private Long topSecretCount;

        /** 精选卷宗数 */
        private Long spotlightCount;

        /** 累计检索次数 */
        private Long searchCount;

        public Stats() {
        }

        public Long getArchiveCount() {
            return archiveCount;
        }

        public void setArchiveCount(Long archiveCount) {
            this.archiveCount = archiveCount;
        }

        public Long getCategoryCount() {
            return categoryCount;
        }

        public void setCategoryCount(Long categoryCount) {
            this.categoryCount = categoryCount;
        }

        public Long getTopSecretCount() {
            return topSecretCount;
        }

        public void setTopSecretCount(Long topSecretCount) {
            this.topSecretCount = topSecretCount;
        }

        public Long getSpotlightCount() {
            return spotlightCount;
        }

        public void setSpotlightCount(Long spotlightCount) {
            this.spotlightCount = spotlightCount;
        }

        public Long getSearchCount() {
            return searchCount;
        }

        public void setSearchCount(Long searchCount) {
            this.searchCount = searchCount;
        }
    }
}
