package com.fenglin.springboottest.service.impl;

import com.fenglin.springboottest.common.CurrentUser;
import com.fenglin.springboottest.common.SecurityLevel;
import com.fenglin.springboottest.entity.DrArchive;
import com.fenglin.springboottest.entity.DrNotice;
import com.fenglin.springboottest.entity.DrUser;
import com.fenglin.springboottest.mapper.DrArchiveCategoryMapper;
import com.fenglin.springboottest.mapper.DrArchiveMapper;
import com.fenglin.springboottest.mapper.DrNoticeMapper;
import com.fenglin.springboottest.mapper.DrSearchLogMapper;
import com.fenglin.springboottest.service.DictService;
import com.fenglin.springboottest.service.HomeService;
import com.fenglin.springboottest.service.SearchService;
import com.fenglin.springboottest.vo.ArchiveVO;
import com.fenglin.springboottest.vo.CategoryVO;
import com.fenglin.springboottest.vo.HomeOverviewVO;
import com.fenglin.springboottest.vo.NoticeVO;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页展示服务实现。
 *
 * <p>首页所有展示内容都来自数据库：学院通报取自 dr_notice，
 * 分类导航取自 dr_archive_category（带可见档案数），
 * 精选 / 热门 / 最新取自 dr_archive，热词取自 dr_hot_keyword。</p>
 *
 * <p>所有数据都按当前登录者的可见密级过滤 —— 不同身份的人看到的首页是不一样的。</p>
 *
 * @author fenglin
 */
@Service
public class HomeServiceImpl implements HomeService {

    private static final DateTimeFormatter NOTICE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 首页各区块展示条数 */
    private static final long LIMIT_NOTICE = 3L;
    private static final long LIMIT_ARCHIVE = 6L;

    private final DrArchiveMapper archiveMapper;
    private final DrArchiveCategoryMapper categoryMapper;
    private final DrNoticeMapper noticeMapper;
    private final DrSearchLogMapper searchLogMapper;
    private final SearchService searchService;
    private final DictService dictService;

    public HomeServiceImpl(DrArchiveMapper archiveMapper,
                           DrArchiveCategoryMapper categoryMapper,
                           DrNoticeMapper noticeMapper,
                           DrSearchLogMapper searchLogMapper,
                           SearchService searchService,
                           DictService dictService) {
        this.archiveMapper = archiveMapper;
        this.categoryMapper = categoryMapper;
        this.noticeMapper = noticeMapper;
        this.searchLogMapper = searchLogMapper;
        this.searchService = searchService;
        this.dictService = dictService;
    }

    @Override
    public HomeOverviewVO overview() {
        int maxLevel = SecurityLevel.maxLevelOfCurrentUser();
        Map<Long, String> categoryNames = loadCategoryNames();

        HomeOverviewVO vo = new HomeOverviewVO();
        vo.setAccess(buildAccess(maxLevel));
        vo.setStats(buildStats(maxLevel));
        vo.setNotices(toNoticeVOList(noticeMapper.listPublished(LIMIT_NOTICE)));
        vo.setCategories(searchService.categories());
        vo.setSpotlight(toArchiveVOList(archiveMapper.listSpotlight(maxLevel, LIMIT_ARCHIVE), categoryNames));
        vo.setHot(toArchiveVOList(archiveMapper.listHot(maxLevel, LIMIT_ARCHIVE), categoryNames));
        vo.setLatest(toArchiveVOList(archiveMapper.listLatest(maxLevel, LIMIT_ARCHIVE), categoryNames));
        vo.setHotKeywords(searchService.hotKeywords());
        return vo;
    }

    // ------------------------------------------------------------------
    // 私有工具
    // ------------------------------------------------------------------

    private HomeOverviewVO.Access buildAccess(int maxLevel) {
        DrUser user = CurrentUser.get();
        HomeOverviewVO.Access access = new HomeOverviewVO.Access();
        access.setLogged(user != null);
        access.setCodename(user == null ? "" : user.getCodename());
        access.setIdentityName(user == null ? "未认证访客" : dictService.identityName(user.getIdentity()));
        access.setMaxLevel(maxLevel);
        access.setMaxLevelLabel(SecurityLevel.label(maxLevel));
        return access;
    }

    private HomeOverviewVO.Stats buildStats(int maxLevel) {
        HomeOverviewVO.Stats stats = new HomeOverviewVO.Stats();
        stats.setArchiveCount(nullToZero(archiveMapper.countVisible(maxLevel)));
        stats.setCategoryCount(nullToZero(categoryMapper.countEnabled()));
        stats.setTopSecretCount(nullToZero(
                archiveMapper.countSearch("", null, null, SecurityLevel.TOP_SECRET, maxLevel)));
        stats.setSpotlightCount(nullToZero(archiveMapper.countSpotlight(maxLevel)));
        stats.setSearchCount(nullToZero(searchLogMapper.countAll()));
        return stats;
    }

    private Map<Long, String> loadCategoryNames() {
        Map<Long, String> names = new LinkedHashMap<>();
        List<CategoryVO> cats = searchService.categories();
        // categories() 返回树，这里扁平化取名字
        for (CategoryVO top : cats) {
            names.put(top.getId(), top.getName());
            if (top.getChildren() != null) {
                for (CategoryVO sub : top.getChildren()) {
                    names.put(sub.getId(), sub.getName());
                }
            }
        }
        // 兜底：若分类表里还有未挂到树上的分类，直接从 Mapper 补齐
        if (names.isEmpty()) {
            categoryMapper.listAllEnabled().forEach(c -> names.put(c.getId(), c.getName()));
        }
        return names;
    }

    private List<NoticeVO> toNoticeVOList(List<DrNotice> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<NoticeVO> list = new ArrayList<>(rows.size());
        for (DrNotice n : rows) {
            NoticeVO vo = new NoticeVO();
            vo.setId(n.getId());
            vo.setTitle(n.getTitle());
            vo.setContent(n.getContent());
            vo.setLevel(n.getLevel() == null ? 1 : n.getLevel());
            vo.setLevelLabel(levelLabel(vo.getLevel()));
            vo.setPinned(n.getPinned() != null && n.getPinned() == 1);
            vo.setPublisher(n.getPublisher());
            vo.setPublishAt(n.getPublishAt() == null ? "" : n.getPublishAt().format(NOTICE_TIME));
            list.add(vo);
        }
        return list;
    }

    private List<ArchiveVO> toArchiveVOList(List<DrArchive> rows, Map<Long, String> categoryNames) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<ArchiveVO> list = new ArrayList<>(rows.size());
        for (DrArchive a : rows) {
            ArchiveVO vo = new ArchiveVO();
            vo.setId(a.getId());
            vo.setCode(a.getCode());
            vo.setTitle(a.getTitle());
            vo.setSummary(a.getSummary());
            vo.setTopCategoryId(a.getTopCategoryId());
            vo.setTopCategoryName(categoryNames.get(a.getTopCategoryId()));
            vo.setCategoryId(a.getCategoryId());
            vo.setCategoryName(categoryNames.get(a.getCategoryId()));
            vo.setSecurityLevel(SecurityLevel.normalize(a.getSecurityLevel()));
            vo.setSecurityLabel(SecurityLevel.label(vo.getSecurityLevel()));
            vo.setRankLabel(a.getRankLabel());
            vo.setTags(splitTags(a.getTags()));
            vo.setSource(a.getSource());
            vo.setViewCount(a.getViewCount());
            vo.setHeat(a.getHeat());
            vo.setSpotlight(a.getSpotlight() != null && a.getSpotlight() == 1);
            list.add(vo);
        }
        return list;
    }

    private String levelLabel(int level) {
        switch (level) {
            case 3:
                return "紧急";
            case 2:
                return "重要";
            default:
                return "通知";
        }
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private Long nullToZero(Long v) {
        return v == null ? 0L : v;
    }
}
