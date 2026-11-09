package com.fenglin.springboottest.service;

import com.fenglin.springboottest.dto.ReplyCreateDTO;
import com.fenglin.springboottest.dto.ReplyUpdateDTO;
import com.fenglin.springboottest.dto.ThreadCreateDTO;
import com.fenglin.springboottest.dto.ThreadUpdateDTO;
import com.fenglin.springboottest.vo.CollectVO;
import com.fenglin.springboottest.vo.ForumOverviewVO;
import com.fenglin.springboottest.vo.LikeVO;
import com.fenglin.springboottest.vo.PageVO;
import com.fenglin.springboottest.vo.ReplyVO;
import com.fenglin.springboottest.vo.ThreadDetailVO;
import com.fenglin.springboottest.vo.ThreadVO;

import java.util.List;

/**
 * 论坛服务。
 *
 * <p>权限设计：不新造一套体系，而是复用现有的两套判定——</p>
 * <ul>
 *   <li>在校身份 → 可见密级上限：{@code SecurityLevel#maxLevelOfCurrentUser()}</li>
 *   <li>版块准入 → 只看在校身份（{@code dr_board.min_identity} / {@code post_min_identity}）</li>
 * </ul>
 * <p>任务系统下线后，版块不再有「权限点」门槛（详见 {@code ForumServiceImpl#canReadBoard}）；
 * 帖子仍用 security_level 定级，不通过时给出「还差什么」而不是干巴巴的 403。</p>
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
     *
     * <p>楼层只返回第 1 页（默认 20 层），后续页走 {@link #replies}。</p>
     */
    ThreadDetailVO detail(Long id);

    /**
     * 主题楼层分页。
     *
     * @param threadId 主题 id
     * @param page     从 1 开始
     * @param size     1–100，超出按 100
     * @return 该页楼层；主题不存在或当前访问者无权查看时抛业务异常
     */
    PageVO<ReplyVO> replies(Long threadId, long page, long size);

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
     * 编辑主题。只有作者本人可以改（教员也不行 —— 事后改写别人的话等于篡改证据，
     * 教员要处理违规内容请走删除或举报下架）。
     *
     * @return 编辑后的详情
     */
    ThreadDetailVO update(Long id, ThreadUpdateDTO dto);

    /**
     * 删除主题（逻辑删除）。作者本人或教员可删。
     *
     * @param reason 删除原因，仅教员删除时会记入日志
     */
    void deleteThread(Long id, String reason);

    /**
     * 编辑楼层。只有该层作者可以改。
     *
     * @return 编辑后的楼层
     */
    ReplyVO updateReply(Long threadId, Long replyId, ReplyUpdateDTO dto);

    /**
     * 删除楼层（逻辑删除）。该层作者或教员可删。
     */
    void deleteReply(Long threadId, Long replyId);

    /**
     * 点赞 / 取消点赞（同一接口切换）。必须登录。
     */
    LikeVO like(Long threadId);

    /**
     * 收藏 / 取消收藏（同一接口切换）。必须登录。
     *
     * <p>与点赞分开计数：点赞进热度，收藏只进个人收藏夹。</p>
     */
    CollectVO collect(Long threadId);

    /**
     * 我的收藏（按收藏时间倒序，需登录）。
     *
     * @param page 从 1 开始
     * @param size 1–50
     */
    PageVO<ThreadVO> collected(long page, long size);

    /**
     * 论坛内检索（标题 / 正文 / 作者），按密级过滤。
     *
     * @param keyword 关键词
     * @param limit   最多返回条数（1–20）
     */
    List<ThreadVO> search(String keyword, int limit);
}
