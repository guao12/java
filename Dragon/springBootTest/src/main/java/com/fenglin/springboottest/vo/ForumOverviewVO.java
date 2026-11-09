package com.fenglin.springboottest.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 论坛首页概览：一次请求返回版块、最新、最热、全站统计与当前访问者信息。
 *
 * <p>与 {@code /api/home/overview} 同样的思路：首页只发一个请求，
 * 避免版块卡片 + 最新 + 最热 + 统计各来一次（首屏四五个并发请求互相抢连接）。</p>
 *
 * @author fenglin
 */
public class ForumOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<BoardVO> boards = new ArrayList<>();

    /** 最新讨论（按最后批注时间） */
    private List<ThreadVO> latest = new ArrayList<>();

    /** 热门讨论（按热度） */
    private List<ThreadVO> hot = new ArrayList<>();

    private Stats stats = new Stats();

    private Me me = new Me();

    public List<BoardVO> getBoards() {
        return boards;
    }

    public void setBoards(List<BoardVO> boards) {
        this.boards = boards;
    }

    public List<ThreadVO> getLatest() {
        return latest;
    }

    public void setLatest(List<ThreadVO> latest) {
        this.latest = latest;
    }

    public List<ThreadVO> getHot() {
        return hot;
    }

    public void setHot(List<ThreadVO> hot) {
        this.hot = hot;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public Me getMe() {
        return me;
    }

    public void setMe(Me me) {
        this.me = me;
    }

    /** 全站统计（已按访问者密级过滤） */
    public static class Stats implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long threadCount = 0L;
        private Long replyCount = 0L;
        private Long todayCount = 0L;
        private Long viewCount = 0L;
        private Long memberCount = 0L;

        public Long getThreadCount() {
            return threadCount;
        }

        public void setThreadCount(Long threadCount) {
            this.threadCount = threadCount;
        }

        public Long getReplyCount() {
            return replyCount;
        }

        public void setReplyCount(Long replyCount) {
            this.replyCount = replyCount;
        }

        public Long getTodayCount() {
            return todayCount;
        }

        public void setTodayCount(Long todayCount) {
            this.todayCount = todayCount;
        }

        public Long getViewCount() {
            return viewCount;
        }

        public void setViewCount(Long viewCount) {
            this.viewCount = viewCount;
        }

        public Long getMemberCount() {
            return memberCount;
        }

        public void setMemberCount(Long memberCount) {
            this.memberCount = memberCount;
        }
    }

    /** 当前访问者 */
    public static class Me implements Serializable {

        private static final long serialVersionUID = 1L;

        private boolean logged;
        private String codename = "";
        private String identity = "";
        private String identityName = "未认证访客";
        private Integer maxLevel = 1;
        private String maxLevelLabel = "公开";
        private Long threadCount = 0L;
        private Long replyCount = 0L;

        public boolean isLogged() {
            return logged;
        }

        public void setLogged(boolean logged) {
            this.logged = logged;
        }

        public String getCodename() {
            return codename;
        }

        public void setCodename(String codename) {
            this.codename = codename;
        }

        public String getIdentity() {
            return identity;
        }

        public void setIdentity(String identity) {
            this.identity = identity;
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

        public Long getThreadCount() {
            return threadCount;
        }

        public void setThreadCount(Long threadCount) {
            this.threadCount = threadCount;
        }

        public Long getReplyCount() {
            return replyCount;
        }

        public void setReplyCount(Long replyCount) {
            this.replyCount = replyCount;
        }
    }
}
