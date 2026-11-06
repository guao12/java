package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.dto.ReplyCreateDTO;
import com.fenglin.springboottest.dto.ReplyUpdateDTO;
import com.fenglin.springboottest.dto.ThreadCreateDTO;
import com.fenglin.springboottest.dto.ThreadUpdateDTO;
import com.fenglin.springboottest.service.ForumService;
import com.fenglin.springboottest.vo.CollectVO;
import com.fenglin.springboottest.vo.ForumOverviewVO;
import com.fenglin.springboottest.vo.LikeVO;
import com.fenglin.springboottest.vo.PageVO;
import com.fenglin.springboottest.vo.ReplyVO;
import com.fenglin.springboottest.vo.ThreadDetailVO;
import com.fenglin.springboottest.vo.ThreadVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
 * GET  /api/forum/threads/{id}            主题详情 + 第 1 页楼层
 * GET  /api/forum/threads/{id}/replies    楼层分页（page, size）
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

    /**
     * 楼层分页。
     *
     * @param page 从 1 开始
     * @param size 1–100（默认 20）
     */
    @GetMapping("/threads/{id}/replies")
    public R<PageVO<ReplyVO>> replies(@PathVariable("id") Long id,
                                      @RequestParam(value = "page", defaultValue = "1") long page,
                                      @RequestParam(value = "size", defaultValue = "20") long size) {
        return R.ok(forumService.replies(id, page, size));
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
        return R.ok("回复已提交", forumService.reply(id, body));
    }

    /** 点赞 / 取消点赞 */
    @PostMapping("/threads/{id}/like")
    public R<LikeVO> like(@PathVariable("id") Long id) {
        return R.ok(forumService.like(id));
    }

    /** 收藏 / 取消收藏 */
    @PostMapping("/threads/{id}/collect")
    public R<CollectVO> collect(@PathVariable("id") Long id) {
        return R.ok(forumService.collect(id));
    }

    /** 我的收藏（需登录） */
    @GetMapping("/collected")
    public R<PageVO<ThreadVO>> collected(@RequestParam(value = "page", defaultValue = "1") long page,
                                         @RequestParam(value = "size", defaultValue = "20") long size) {
        return R.ok(forumService.collected(page, size));
    }

    /** 编辑主题（仅作者本人） */
    @PutMapping("/threads/{id}")
    public R<ThreadDetailVO> update(@PathVariable("id") Long id,
                                    @Valid @RequestBody ThreadUpdateDTO body) {
        return R.ok("已保存", forumService.update(id, body));
    }

    /** 删除主题（作者本人或版务） */
    @DeleteMapping("/threads/{id}")
    public R<Void> deleteThread(@PathVariable("id") Long id,
                                @RequestParam(value = "reason", required = false) String reason) {
        forumService.deleteThread(id, reason);
        return R.ok("主题已删除", null);
    }

    /** 编辑楼层（仅该层作者） */
    @PutMapping("/threads/{id}/replies/{replyId}")
    public R<ReplyVO> updateReply(@PathVariable("id") Long id,
                                  @PathVariable("replyId") Long replyId,
                                  @Valid @RequestBody ReplyUpdateDTO body) {
        return R.ok("已保存", forumService.updateReply(id, replyId, body));
    }

    /** 删除楼层（该层作者或版务） */
    @DeleteMapping("/threads/{id}/replies/{replyId}")
    public R<Void> deleteReply(@PathVariable("id") Long id,
                               @PathVariable("replyId") Long replyId) {
        forumService.deleteReply(id, replyId);
        return R.ok("回复已删除", null);
    }

    /** 论坛内检索 */
    @GetMapping("/search")
    public R<List<ThreadVO>> search(@RequestParam("kw") String kw,
                                    @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return R.ok(forumService.search(kw, limit));
    }
}
