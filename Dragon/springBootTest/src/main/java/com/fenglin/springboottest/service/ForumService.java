package com.fenglin.springboottest.service;

import com.fenglin.springboottest.dto.ReplyCreateDTO;
import com.fenglin.springboottest.dto.ThreadCreateDTO;
import com.fenglin.springboottest.vo.ForumOverviewVO;
import com.fenglin.springboottest.vo.LikeVO;
import com.fenglin.springboottest.vo.PageVO;
import com.fenglin.springboottest.vo.ThreadDetailVO;
import com.fenglin.springboottest.vo.ThreadVO;

import java.util.List;

/**
 * 论坛服务。
 *
 * <p>权限设计：不新造一套体系，而是复用现有的两套判定——</p>
 * <ul>
 *   <li>在校身份 → 可见密级上限：{@code SecurityLevel#maxLevelOfCurrentUser()}</li>
 *   <li>权限点：{@code dr_user_clearance.points}（由执行部任务累积）</li>
 * </ul>
 * <p>版块用 min_identity / min_points 把门，帖子用 security_level 定级，
 * 两者都不通过时给出「还差什么」而不是干巴巴的 403。</p>
 *
 * <p>写操作（发帖 / 回帖 / 点赞）一律要求登录，见各方法说明。</p>
 *
 * @author fenglin
 */
public interface ForumService {

    /**
     * 论坛首页概览：版块 + 最新 + 最热 + 统计 + 当前访问者。
     * 对未登录访客同样可用（按公开级过滤）。
     */
    ForumOverviewVO overview();

    /**
     * 版块分页列表。
     *
     * @param boardCode 版块 code
     * @param sort      latest（最新批注）/ hot（热度）/ essence（仅入藏）
     * @param keyword   optional 关键词（标题 / 正文 / 作者）
     * @param page      从 1 开始
     * @param size      1–50，超出按 50
     */
    PageVO<ThreadVO> threads(String boardCode, String sort, String keyword, long page, long size);

    /**
     * 主题详情（含楼层）。会顺带把浏览量 +1。
     * 密级不足或「回复可见」未解锁时，正文为空并给出 hiddenReason，标题仍然返回。
     */
    ThreadDetailVO detail(Long id);

    /**
     * 发帖。必须登录；校验版块可读可发、标题正文长度、发帖频率、敏感词。
     * 密级不能高于发布者自身可读上限（否则自己都看不见自己的帖子）。
     */
    ThreadDetailVO create(ThreadCreateDTO dto);

    /**
     * 回帖。必须登录；校验主题可读、批注长度、回帖频率、引用合法性。
     */
    ThreadDetailVO reply(Long threadId, ReplyCreateDTO dto);

    /**
     * 点赞 / 取消点赞（同一接口切换）。必须登录。
     */
    LikeVO like(Long threadId);

    /**
     * 论坛内检索（标题 / 正文 / 作者），按密级过滤。
     *
     * @param keyword 关键词
     * @param limit   最多返回条数（1–20）
     */
    List<ThreadVO> search(String keyword, int limit);
}
