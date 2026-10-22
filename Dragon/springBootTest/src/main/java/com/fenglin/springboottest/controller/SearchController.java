package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.dto.SearchQueryDTO;
import com.fenglin.springboottest.service.SearchService;
import com.fenglin.springboottest.vo.ArchiveVO;
import com.fenglin.springboottest.vo.CategoryVO;
import com.fenglin.springboottest.vo.SearchResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 档案检索接口：关键词 + 两级分类 + 密级分级 + 排序 + 分页。
 *
 * <pre>
 * GET /api/search?keyword=&topCategory=&category=&level=&sort=&page=&size=
 * GET /api/search/categories   两级分类树（带可见档案数）
 * GET /api/search/hot          热门检索词
 * GET /api/archive/{id}        卷宗详情
 * </pre>
 *
 * <p>密级分級：结果始终按访问者身份的可见上限过滤，越级卷宗不会出现；
 * level 参数若超过可见上限会被自动收敛。</p>
 *
 * @author fenglin
 */
@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * 分级分类检索。
     *
     * @param keyword     关键词，匹配标题 / 摘要 / 标签 / 卷宗编号
     * @param topCategory 一级分类 ID
     * @param category    二级分类 ID
     * @param level       密级 1-4（公开 / 内部 / 机密 / 绝密）
     * @param sort        排序：heat（默认） / new / view / level
     * @param page        页码，从 1 开始
     * @param size        每页条数，最大 50
     */
    @GetMapping("/search")
    public R<SearchResultVO> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long topCategory,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false, defaultValue = "heat") String sort,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "12") Integer size) {

        SearchQueryDTO query = new SearchQueryDTO();
        query.setKeyword(keyword);
        query.setTopCategory(topCategory);
        query.setCategory(category);
        query.setLevel(level);
        query.setSort(sort);
        query.setPage(page);
        query.setSize(size);
        return R.ok(searchService.search(query));
    }

    /** 两级分类树，含当前访问者可见的档案数 */
    @GetMapping("/search/categories")
    public R<List<CategoryVO>> categories() {
        return R.ok(searchService.categories());
    }

    /** 热门检索词 */
    @GetMapping("/search/hot")
    public R<List<String>> hot() {
        return R.ok(searchService.hotKeywords());
    }

    /** 卷宗详情，浏览量 +1；越级访问返回业务错误 */
    @GetMapping("/archive/{id}")
    public R<ArchiveVO> detail(@PathVariable("id") Long id) {
        return R.ok(searchService.detail(id));
    }
}
