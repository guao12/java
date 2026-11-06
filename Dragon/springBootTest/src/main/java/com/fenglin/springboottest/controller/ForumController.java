package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.dto.ReplyCreateDTO;
import com.fenglin.springboottest.dto.ThreadCreateDTO;
import com.fenglin.springboottest.service.ForumService;
import com.fenglin.springboottest.vo.ForumOverviewVO;
import com.fenglin.springboottest.vo.LikeVO;
import com.fenglin.springboottest.vo.PageVO;
import com.fenglin.springboottest.vo.ThreadDetailVO;
import com.fenglin.springboottest.vo.ThreadVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 论坛接口。
 *
 * <pre>
 * GET  /api/forum/overview                首页概览（版块 / 最新 / 最热 / 统计 / 我的权限）
 * GET  /api/forum/threads                 版块分页列表（board, sort, kw, page, size）
 * GET  /api/forum/threads/{id}            主题详情 + 楼层
 * POST /api/forum/threads                 发帖（需登录）
 * POST /api/forum/threads/{id}/replies    回帖（需登录）
 * POST /api/forum/threads/{id}/like       点赞 / 取消点赞（需登录）
 * GET  /api/forum/search                  论坛内检索
 * </pre>
 *
 * <p>整个 /api/forum/** 挂的是软鉴权（{@code OptionalAuthInterceptor}）：
 * 读接口对未登录访客也放行，只是只能看公开级；写接口在 Service 里强制要求登录，
 * 未登录返回 code=401，前端会自动跳登录页。</p>
 *
 * @author fenglin
 */
@RestController
@RequestMapping("/api/forum")
public class ForumController {

    private final ForumService forumService;

    public ForumController(ForumService forumService) {
        this.forumService = forumService;
    }

    /** 首页概览 */
    @GetMapping("/overview")
    public R<ForumOverviewVO> overview() {
        return R.ok(forumService.overview());
    }

    /**
     * 版块分页列表。
     *
     * @param board 版块 code
     * @param sort  latest / hot / essence
     * @param kw    可选关键词
     * @param page  从 1 开始
     * @param size  1–50
     */
    @GetMapping("/threads")
    public R<PageVO<ThreadVO>> threads(@RequestParam("board") String board,
                                       @RequestParam(value = "sort", defaultValue = "latest") String sort,
                                       @RequestParam(value = "kw", required = false) String kw,
                                       @RequestParam(value = "page", defaultValue = "1") long page,
                                       @RequestParam(value = "size", defaultValue = "20") long size) {
        return R.ok(forumService.threads(board, sort, kw, page, size));
    }

    /** 主题详情 */
    @GetMapping("/threads/{id}")
    public R<ThreadDetailVO> detail(@PathVariable("id") Long id) {
        return R.ok(forumService.detail(id));
    }

    /** 发帖 */
    @PostMapping("/threads")
    public R<ThreadDetailVO> create(@Valid @RequestBody ThreadCreateDTO body) {
        return R.ok("已发布", forumService.create(body));
    }

    /** 回帖 */
    @PostMapping("/threads/{id}/replies")
    public R<ThreadDetailVO> reply(@PathVariable("id") Long id,
                                   @Valid @RequestBody ReplyCreateDTO body) {
        return R.ok("批注已提交", forumService.reply(id, body));
    }

    /** 点赞 / 取消点赞 */
    @PostMapping("/threads/{id}/like")
    public R<LikeVO> like(@PathVariable("id") Long id) {
        return R.ok(forumService.like(id));
    }

    /** 论坛内检索 */
    @GetMapping("/search")
    public R<List<ThreadVO>> search(@RequestParam("kw") String kw,
                                    @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return R.ok(forumService.search(kw, limit));
    }
}
