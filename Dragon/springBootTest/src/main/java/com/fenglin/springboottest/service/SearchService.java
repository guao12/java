package com.fenglin.springboottest.service;

import com.fenglin.springboottest.dto.SearchQueryDTO;
import com.fenglin.springboottest.vo.ArchiveVO;
import com.fenglin.springboottest.vo.CategoryVO;
import com.fenglin.springboottest.vo.SearchResultVO;

import java.util.List;

/**
 * 档案检索服务：关键词 + 两级分类 + 密级分级 + 排序 + 分页。
 *
 * @author fenglin
 */
public interface SearchService {

    /**
     * 分级分类检索。
     *
     * <p>结果始终按当前登录者的可见密级上限过滤，越级卷宗不会出现在结果里。</p>
     */
    SearchResultVO search(SearchQueryDTO query);

    /** 两级分类树（带当前访问者可见的档案数） */
    List<CategoryVO> categories();

    /** 热门检索词 */
    List<String> hotKeywords();

    /** 卷宗详情（浏览量 +1，越级访问直接抛业务异常） */
    ArchiveVO detail(Long id);
}
