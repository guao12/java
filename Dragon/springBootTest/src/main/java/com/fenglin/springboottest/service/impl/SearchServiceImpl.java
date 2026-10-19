package com.fenglin.springboottest.service.impl;

import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.common.CurrentUser;
import com.fenglin.springboottest.common.SecurityLevel;
import com.fenglin.springboottest.dto.SearchQueryDTO;
import com.fenglin.springboottest.entity.DrArchive;
import com.fenglin.springboottest.entity.DrArchiveCategory;
import com.fenglin.springboottest.entity.DrSearchLog;
import com.fenglin.springboottest.entity.DrUser;
import com.fenglin.springboottest.mapper.DrArchiveCategoryMapper;
import com.fenglin.springboottest.mapper.DrArchiveMapper;
import com.fenglin.springboottest.mapper.DrHotKeywordMapper;
import com.fenglin.springboottest.mapper.DrSearchLogMapper;
import com.fenglin.springboottest.service.SearchService;
import com.fenglin.springboottest.vo.ArchiveVO;
import com.fenglin.springboottest.vo.CategoryVO;
import com.fenglin.springboottest.vo.FacetVO;
import com.fenglin.springboottest.vo.PageVO;
import com.fenglin.springboottest.vo.SearchResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 档案检索服务实现。
 *
 * <p><b>分级</b>指密级：每次检索都按当前登录者身份确定可见上限 {@code securityLevel <= maxLevel}；
 * <b>分类</b>指两级分类树：一级（人物志 / 言灵谱…）+ 二级（混血种名册 / 龙王系谱…）。</p>
 *
 * @author fenglin
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 关键词最大长度，与前端 maxlength 保持一致 */
    private static final int KEYWORD_MAX = 40;

    /** 每页条数上限，防止一次性拉取全表 */
    private static final int SIZE_MAX = 50;

    private static final long LIMIT_DEFAULT = 12L;

    private final DrArchiveMapper archiveMapper;
    private final DrArchiveCategoryMapper categoryMapper;
    private final DrHotKeywordMapper hotKeywordMapper;
    private final DrSearchLogMapper searchLogMapper;

    public SearchServiceImpl(DrArchiveMapper archiveMapper,
                             DrArchiveCategoryMapper categoryMapper,
                             DrHotKeywordMapper hotKeywordMapper,
                             DrSearchLogMapper searchLogMapper) {
        this.archiveMapper = archiveMapper;
        this.categoryMapper = categoryMapper;
        this.hotKeywordMapper = hotKeywordMapper;
        this.searchLogMapper = searchLogMapper;
    }

    // ------------------------------------------------------------------
    // 检索
    // ------------------------------------------------------------------

    @Override
    public SearchResultVO search(SearchQueryDTO query) {
        SearchQueryDTO q = query == null ? new SearchQueryDTO() : query;

        int maxLevel = SecurityLevel.maxLevelOfCurrentUser();

        // 1) 归一化参数
        String kw = normalizeKeyword(q.getKeyword());
        Integer level = q.getLevel() == null ? null : SecurityLevel.normalize(q.getLevel());
        // 越级的密级筛选直接收敛到可见上限，避免前端传 4 就绕过
        if (level != null && level > maxLevel) {
            level = maxLevel;
        }
        long page = q.getPage() == null || q.getPage() < 1 ? 1L : q.getPage();
        long size = q.getSize() == null || q.getSize() < 1 ? LIMIT_DEFAULT : Math.min(q.getSize(), SIZE_MAX);
        String sort = q.getSort() == null ? "heat" : q.getSort();

        // 2) 总数与分页列表
        Long total = archiveMapper.countSearch(kw, q.getTopCategory(), q.getCategory(), level, maxLevel);
        long offset = (page - 1) * size;
        List<DrArchive> rows = archiveMapper.listSearch(
                kw, q.getTopCategory(), q.getCategory(), level, maxLevel, orderByOf(sort), offset, size);

        // 3) 分类名映射（分类很少，一次查全再在内存里拼装，避免 N 次 join）
        Map<Long, String> categoryNames = loadCategoryNames();

        SearchResultVO result = new SearchResultVO();
        result.setTotal(total);
        result.setPage(new PageVO<>(toVOList(rows, categoryNames, false), total, page, size));

        // 4) 分面统计：三个维度口径一致 —— 都只对「其他维度」生效，忽略自身维度
        result.setTopCategoryFacets(archiveMapper.facetTopCategory(kw, level, maxLevel));
        result.setCategoryFacets(archiveMapper.facetCategory(kw, level, maxLevel));
        result.setLevelFacets(fillLevelFacets(archiveMapper.facetLevel(kw, q.getTopCategory(), q.getCategory(), maxLevel), maxLevel));

        // 5) 条件回显
        SearchResultVO.Query echo = new SearchResultVO.Query();
        echo.setKeyword(kw);
        echo.setTopCategory(q.getTopCategory());
        echo.setCategory(q.getCategory());
        echo.setLevel(level);
        echo.setSort(sort);
        result.setEcho(echo);

        // 6) 记录检索日志 + 命中热词加热（失败不影响检索结果）
        writeLog(kw, q.getTopCategory(), q.getCategory(), q.getLevel(), total == null ? 0 : total.intValue());

        return result;
    }

    // ------------------------------------------------------------------
    // 分类树 / 热词 / 详情
    // ------------------------------------------------------------------

    @Override
    public List<CategoryVO> categories() {
        int maxLevel = SecurityLevel.maxLevelOfCurrentUser();

        List<DrArchiveCategory> all = categoryMapper.listAllEnabled();

        // 各分类下的可见档案数
        Map<Long, Long> topCounts = toCountMap(archiveMapper.countByTopCategory(maxLevel));
        Map<Long, Long> subCounts = toCountMap(archiveMapper.countByCategory(maxLevel));

        Map<Long, CategoryVO> topMap = new LinkedHashMap<>();
        List<CategoryVO> tops = new ArrayList<>();

        // 先建一级
        for (DrArchiveCategory c : all) {
            if (c.getParentId() != null && c.getParentId() == 0L) {
                CategoryVO vo = toCategoryVO(c, topCounts.getOrDefault(c.getId(), 0L));
                vo.setChildren(new ArrayList<>());
                topMap.put(c.getId(), vo);
                tops.add(vo);
            }
        }
        // 再挂二级
        for (DrArchiveCategory c : all) {
            if (c.getParentId() == null || c.getParentId() == 0L) {
                continue;
            }
            CategoryVO parent = topMap.get(c.getParentId());
            if (parent == null) {
                continue; // 父级被停用的孤儿分类，直接跳过
            }
            parent.getChildren().add(toCategoryVO(c, subCounts.getOrDefault(c.getId(), 0L)));
        }
        return tops;
    }

    @Override
    public List<String> hotKeywords() {
        return hotKeywordMapper.listTop(8).stream()
                .map(k -> k.getKeyword())
                .collect(Collectors.toList());
    }

    @Override
    public ArchiveVO detail(Long id) {
        if (id == null) {
            throw new BizException("缺少卷宗编号");
        }
        DrArchive archive = archiveMapper.selectVisibleById(id);
        if (archive == null) {
            throw new BizException("卷宗不存在或已被销毁");
        }
        if (archive.getStatus() == null || archive.getStatus() == 0) {
            throw new BizException("该卷宗已被封存，暂不可查阅");
        }

        int maxLevel = SecurityLevel.maxLevelOfCurrentUser();
        int archiveLevel = SecurityLevel.normalize(archive.getSecurityLevel());
        if (archiveLevel > maxLevel) {
            throw new BizException("权限不足：该卷宗为「" + SecurityLevel.label(archiveLevel)
                    + "」级，当前身份仅可查阅「" + SecurityLevel.label(maxLevel) + "」及以下卷宗");
        }

        // 浏览量 +1
        archiveMapper.incrViewCount(id);

        Map<Long, String> categoryNames = loadCategoryNames();
        ArchiveVO vo = toVO(archive, categoryNames, true);
        vo.setViewCount((archive.getViewCount() == null ? 0 : archive.getViewCount()) + 1);
        return vo;
    }

    // ------------------------------------------------------------------
    // 私有工具
    // ------------------------------------------------------------------

    /** 排序字段白名单：绝不接受前端传入的原始列名 */
    private String orderByOf(String sort) {
        if (sort == null) {
            return "a.heat DESC, a.id ASC";
        }
        switch (sort) {
            case "new":
                return "a.create_time DESC, a.id DESC";
            case "view":
                return "a.view_count DESC, a.id ASC";
            case "level":
                return "a.security_level DESC, a.heat DESC, a.id ASC";
            case "heat":
            default:
                return "a.heat DESC, a.id ASC";
        }
    }

    private String normalizeKeyword(String raw) {
        if (raw == null) {
            return "";
        }
        String kw = raw.trim();
        if (kw.length() > KEYWORD_MAX) {
            kw = kw.substring(0, KEYWORD_MAX);
        }
        // 去掉 LIKE 通配符，避免用户输入 % 就全表命中
        return kw.replace("%", "").replace("_", "");
    }

    /** 密级分面补齐到可见上限，保证前端渲染的筛选项稳定 */
    private List<FacetVO> fillLevelFacets(List<FacetVO> facets, int maxLevel) {
        Map<Integer, Long> exist = new LinkedHashMap<>();
        if (facets != null) {
            for (FacetVO f : facets) {
                exist.put(f.getKey() == null ? 0 : f.getKey().intValue(), f.getTotal());
            }
        }
        List<FacetVO> out = new ArrayList<>();
        for (int lv = SecurityLevel.PUBLIC; lv <= maxLevel; lv++) {
            out.add(new FacetVO((long) lv, SecurityLevel.label(lv), exist.getOrDefault(lv, 0L)));
        }
        return out;
    }

    private Map<Long, String> loadCategoryNames() {
        List<DrArchiveCategory> all = categoryMapper.listAllEnabled();
        Map<Long, String> names = new LinkedHashMap<>(all.size());
        for (DrArchiveCategory c : all) {
            names.put(c.getId(), c.getName());
        }
        return names;
    }

    private Map<Long, Long> toCountMap(List<Map<String, Object>> rows) {
        Map<Long, Long> map = new LinkedHashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            Object total = row.get("total");
            if (id == null) {
                continue;
            }
            long t = 0L;
            if (total instanceof Number) {
                t = ((Number) total).longValue();
            }
            map.put(((Number) id).longValue(), t);
        }
        return map;
    }

    private CategoryVO toCategoryVO(DrArchiveCategory c, Long count) {
        CategoryVO vo = new CategoryVO();
        vo.setId(c.getId());
        vo.setParentId(c.getParentId());
        vo.setCode(c.getCode());
        vo.setName(c.getName());
        vo.setIcon(c.getIcon());
        vo.setSortNo(c.getSortNo());
        vo.setCount(count);
        return vo;
    }

    private List<ArchiveVO> toVOList(List<DrArchive> rows, Map<Long, String> categoryNames, boolean withContent) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<ArchiveVO> list = new ArrayList<>(rows.size());
        for (DrArchive a : rows) {
            list.add(toVO(a, categoryNames, withContent));
        }
        return list;
    }

    private ArchiveVO toVO(DrArchive a, Map<Long, String> categoryNames, boolean withContent) {
        ArchiveVO vo = new ArchiveVO();
        vo.setId(a.getId());
        vo.setCode(a.getCode());
        vo.setTitle(a.getTitle());
        vo.setSummary(a.getSummary());
        if (withContent) {
            vo.setContent(a.getContent());
        }
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
        vo.setCreateTime(a.getCreateTime() == null ? "" : a.getCreateTime().format(TIME_FMT));
        return vo;
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

    /** 检索日志 + 热词加热。任何异常都吞掉，绝不影响检索主流程。 */
    private void writeLog(String keyword, Long topCategory, Long category, Integer level, int resultCount) {
        try {
            DrUser user = CurrentUser.get();
            DrSearchLog logRow = new DrSearchLog();
            logRow.setUserId(user == null ? null : user.getId());
            logRow.setKeyword(keyword == null ? "" : keyword);
            logRow.setTopCategory(topCategory);
            logRow.setCategoryId(category);
            logRow.setLevel(level);
            logRow.setResultCount(resultCount);
            logRow.setClientIp(clientIp());
            searchLogMapper.insert(logRow);

            if (keyword != null && !keyword.isEmpty()) {
                hotKeywordMapper.bumpHeat(keyword);
            }
        } catch (Exception e) {
            log.warn("检索日志写入失败（不影响检索结果）：{}", e.getMessage());
        }
    }

    private String clientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? "" : String.valueOf(attrs.getRequest().getRemoteAddr());
        } catch (Exception e) {
            return "";
        }
    }
}
