package com.fenglin.springboottest.service.impl;

import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.common.CurrentUser;
import com.fenglin.springboottest.common.SecurityLevel;
import com.fenglin.springboottest.dto.ReplyCreateDTO;
import com.fenglin.springboottest.dto.ReplyUpdateDTO;
import com.fenglin.springboottest.dto.ThreadCreateDTO;
import com.fenglin.springboottest.dto.ThreadUpdateDTO;
import com.fenglin.springboottest.entity.DrBoard;
import com.fenglin.springboottest.entity.DrReply;
import com.fenglin.springboottest.entity.DrThread;
import com.fenglin.springboottest.entity.DrThreadAction;
import com.fenglin.springboottest.entity.DrUser;
import com.fenglin.springboottest.mapper.DrBoardMapper;
import com.fenglin.springboottest.mapper.DrReplyMapper;
import com.fenglin.springboottest.mapper.DrThreadActionMapper;
import com.fenglin.springboottest.mapper.DrThreadMapper;
import com.fenglin.springboottest.mapper.DrUserMapper;
import com.fenglin.springboottest.service.DictService;
import com.fenglin.springboottest.service.ForumService;
import com.fenglin.springboottest.vo.BoardVO;
import com.fenglin.springboottest.vo.CollectVO;
import com.fenglin.springboottest.vo.ForumOverviewVO;
import com.fenglin.springboottest.vo.LikeVO;
import com.fenglin.springboottest.vo.PageVO;
import com.fenglin.springboottest.vo.ReplyVO;
import com.fenglin.springboottest.vo.ThreadDetailVO;
import com.fenglin.springboottest.vo.ThreadVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 论坛服务实现。
 *
 * <p>三处刻意的设计：</p>
 * <ol>
 *   <li><b>权限解释化</b>：任何拒绝都带「还差什么」（身份 / 权限点 / 密级），
 *       而不是只回一句 403。用户看得懂才有动力去刷任务。</li>
 *   <li><b>正文按需返回</b>：列表接口永远不返回正文，详情接口在密级不足、
 *       回复可见未解锁时也不返回。避免用列表接口绕过权限。</li>
 *   <li><b>计数同事务</b>：reply_count / last_reply_at / heat 与插入在同一事务里更新，
 *       否则排序会漂移（旧帖排到最前、热度不涨）。</li>
 * </ol>
 *
 * @author fenglin
 */
@Service
public class ForumServiceImpl implements ForumService {

    /** ISO 本地时间：前端 new Date(...) 在 iOS Safari 也能正确解析 */
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final long DEFAULT_SIZE = 20L;
    private static final long MAX_SIZE = 50L;

    /** 楼层分页：默认一页 20 层，上限 100 层（防止前端传 size=100000 变相全量拉取） */
    private static final long REPLY_PAGE_SIZE = 20L;
    private static final long REPLY_MAX_SIZE = 100L;

    /** 热榜时间窗：只看最近 7 天有动静的帖子，避免历史总榜霸占首页 */
    private static final int HOT_WINDOW_HOURS = 168;

    /** 发帖间隔：30 秒内只能发一帖 */
    private static final long POST_INTERVAL_SECONDS = 30L;
    /** 回帖间隔：5 秒内只能发一条，挡连点与脚本 */
    private static final long REPLY_INTERVAL_SECONDS = 5L;

    /** 极简敏感词表：只挡最明显的广告与引流，不做过度过滤 */
    private static final String[] BANNED_WORDS = {
            "加微信", "加V", "代购", "刷单", "博彩", "办证", "私聊报价"
    };

    private final DrBoardMapper boardMapper;
    private final DrThreadMapper threadMapper;
    private final DrReplyMapper replyMapper;
    private final DrThreadActionMapper actionMapper;
    private final DrUserMapper userMapper;
    private final DictService dictService;

    public ForumServiceImpl(DrBoardMapper boardMapper,
                            DrThreadMapper threadMapper,
                            DrReplyMapper replyMapper,
                            DrThreadActionMapper actionMapper,
                            DrUserMapper userMapper,
                            DictService dictService) {
        this.boardMapper = boardMapper;
        this.threadMapper = threadMapper;
        this.replyMapper = replyMapper;
        this.actionMapper = actionMapper;
        this.userMapper = userMapper;
        this.dictService = dictService;
    }

    // ==================================================================
    // 概览
    // ==================================================================

    @Override
    public ForumOverviewVO overview() {
        DrUser user = CurrentUser.get();
        int maxLevel = SecurityLevel.maxLevelOfCurrentUser();
        int rank = rankOf(user);

        List<DrBoard> boards = boardMapper.listOpen();
        Map<Long, DrBoard> index = boards.stream()
                .collect(Collectors.toMap(DrBoard::getId, b -> b, (a, b) -> a));
        Map<Long, Integer> counts = countMap(threadMapper.countGroupByBoard(maxLevel));

        ForumOverviewVO vo = new ForumOverviewVO();
        vo.setBoards(boards.stream()
                .map(b -> toBoardVO(b, rank, counts.getOrDefault(b.getId(), 0)))
                .collect(Collectors.toList()));

        List<DrThread> latestRows = threadMapper.listLatest(maxLevel, 8);
        List<DrThread> hotRows = hotRows(maxLevel, 8);
        vo.setLatest(toThreadVOs(latestRows, index, user, maxLevel));
        vo.setHot(toThreadVOs(hotRows, index, user, maxLevel));

        ForumOverviewVO.Stats stats = new ForumOverviewVO.Stats();
        stats.setThreadCount(nz(threadMapper.countVisible(maxLevel)));
        stats.setReplyCount(nz(replyMapper.countVisible(maxLevel)));
        stats.setTodayCount(nz(threadMapper.countToday(maxLevel)));
        stats.setViewCount(nz(threadMapper.sumVisibleViews(maxLevel)));
        stats.setMemberCount(nz(userMapper.selectCount(null)));
        vo.setStats(stats);

        ForumOverviewVO.Me me = new ForumOverviewVO.Me();
        me.setMaxLevel(maxLevel);
        me.setMaxLevelLabel(SecurityLevel.label(maxLevel));
        if (user != null) {
            me.setLogged(true);
            me.setCodename(safe(user.getCodename()));
            me.setIdentity(safe(user.getIdentity()));
            me.setIdentityName(dictService.identityName(user.getIdentity()));
            me.setThreadCount(nz(threadMapper.countByAuthor(user.getId())));
            me.setReplyCount(nz(replyMapper.countByAuthor(user.getId())));
        }
        vo.setMe(me);
        return vo;
    }

    /**
     * 热榜取数：先用「近 7 天有活动」的帖子按 heat 排，不足 limit 时用全时段热榜补齐（去重）。
     *
     * <p>为什么不做硬过滤：论坛冷清时 7 天窗口可能只剩两三条，
     * 热榜空一半比「有点旧内容」更难看。先给新鲜内容让路，再用旧内容兜底。</p>
     */
    private List<DrThread> hotRows(int maxLevel, int limit) {
        List<DrThread> rows = threadMapper.listHotRecent(maxLevel, HOT_WINDOW_HOURS, limit);
        if (rows.size() >= limit) {
            return rows;
        }
        Set<Long> seen = rows.stream().map(DrThread::getId).collect(Collectors.toSet());
        for (DrThread t : threadMapper.listHot(maxLevel, limit)) {
            if (seen.add(t.getId())) {
                rows.add(t);
            }
            if (rows.size() >= limit) {
                break;
            }
        }
        return rows;
    }

    // ==================================================================
    // 版块列表
    // ==================================================================

    @Override
    public PageVO<ThreadVO> threads(String boardCode, String sort, String keyword, long page, long size) {
        DrBoard board = boardMapper.selectByCode(boardCode);
        if (board == null) {
            throw new BizException("版块不存在");
        }
        DrUser user = CurrentUser.get();
        int rank = rankOf(user);
        if (!canReadBoard(board, rank)) {
            throw new BizException(403, "无权进入该版块 · " + lockHint(board, rank));
        }

        long p = Math.max(1L, page);
        long s = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String kw = trimToNull(keyword);
        boolean essenceOnly = "essence".equals(sort);

        Long total = threadMapper.countByBoard(board.getId(), kw, essenceOnly);
        List<ThreadVO> list = Collections.emptyList();
        if (total != null && total > 0) {
            long offset = (p - 1) * s;
            List<DrThread> rows = threadMapper.listByBoard(
                    board.getId(), kw, essenceOnly, orderByOf(sort), offset, s);
            Map<Long, DrBoard> index = Collections.singletonMap(board.getId(), board);
            list = toThreadVOs(rows, index, user, SecurityLevel.maxLevelOfCurrentUser());
        }
        return new PageVO<>(list, total == null ? 0L : total, p, s);
    }

    // ==================================================================
    // 详情
    // ==================================================================

    @Override
    public ThreadDetailVO detail(Long id) {
        DrThread row = threadMapper.selectLive(id);
        if (row == null) {
            throw new BizException("主题不存在或已下线");
        }
        DrUser user = CurrentUser.get();
        int maxLevel = SecurityLevel.maxLevelOfCurrentUser();
        int rank = rankOf(user);
        DrBoard board = boardMapper.selectById(row.getBoardId());

        ThreadDetailVO out = new ThreadDetailVO();
        out.setMaxLevel(maxLevel);
        out.setViewerStaff(isStaff(user));
        out.setThread(toThreadVO(row, board, user, maxLevel,
                isLiked(row.getId(), user), isCollected(row.getId(), user)));

        boolean boardOk = board != null && canReadBoard(board, rank);
        boolean levelOk = row.getSecurityLevel() == null || row.getSecurityLevel() <= maxLevel;
        boolean replyVisible = row.getReplyVisible() != null && row.getReplyVisible() == 1;
        boolean replied = user != null && hasReplied(row.getId(), user.getId());

        if (!boardOk) {
            out.setContentHidden(true);
            out.setHiddenReason("该版块需要更高权限才能进入，正文未予展示");
            out.setCanReply(false);
            out.setReplyHint("无权在本版块回复");
            return out;
        }
        if (!levelOk) {
            out.setContentHidden(true);
            out.setHiddenReason("本文密级为「" + SecurityLevel.label(row.getSecurityLevel())
                    + "」，当前身份可读至「" + SecurityLevel.label(maxLevel) + "」");
            out.setCanReply(false);
            out.setReplyHint("密级不足，无法回复");
            return out;
        }

        // 楼层先组装（「回复可见」未解锁时也要能看到已有讨论，否则用户会以为整帖是空的）
        // 只带第 1 页：楼层多的帖子由前端按 /threads/{id}/replies 翻页
        PageVO<ReplyVO> firstPage = buildReplyPage(row.getId(), user, 1, REPLY_PAGE_SIZE);
        out.setReplies(firstPage.getList());
        out.setReplyTotal(firstPage.getTotal());
        out.setReplyPage(firstPage.getPage());
        out.setReplySize(firstPage.getSize());
        out.setReplyPages(firstPage.getPages());
        out.setReplyUnlocked(!replyVisible || replied);
        out.setCanReply(user != null);
        if (user == null) {
            out.setReplyHint("请先完成身份认证后再回复");
        }

        if (replyVisible && !replied) {
            out.setContentHidden(true);
            out.setHiddenReason("回复本条主题后可见正文");
        } else {
            out.setContent(row.getContent());
            // 只在正文真正返回时计一次浏览，避免被反复刷新刷高
            threadMapper.incrView(row.getId());
            row.setViewCount((row.getViewCount() == null ? 0 : row.getViewCount()) + 1);
            out.getThread().setViewCount(row.getViewCount());
        }
        return out;
    }

    /**
     * 楼层翻页。
     *
     * <p>必须重做一次可见性判定，不能只信 threadId：否则密级不足的人
     * 可以绕开 detail() 的隐藏逻辑，直接用这个接口把整帖楼层翻出来。</p>
     */
    @Override
    public PageVO<ReplyVO> replies(Long threadId, long page, long size) {
        DrThread row = threadMapper.selectLive(threadId);
        if (row == null) {
            throw new BizException("主题不存在或已下线");
        }
        DrUser user = CurrentUser.get();
        int maxLevel = SecurityLevel.maxLevelOfCurrentUser();
        DrBoard board = boardMapper.selectById(row.getBoardId());
        int rank = rankOf(user);

        boolean boardOk = board != null && canReadBoard(board, rank);
        boolean levelOk = row.getSecurityLevel() == null || row.getSecurityLevel() <= maxLevel;
        if (!boardOk || !levelOk) {
            throw new BizException(403, "无权查看该主题的楼层");
        }
        return buildReplyPage(threadId, user, page, size);
    }

    // ==================================================================
    // 发帖
    // ==================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ThreadDetailVO create(ThreadCreateDTO dto) {
        DrUser user = requireLogin();
        DrBoard board = boardMapper.selectByCode(dto.getBoardCode());
        if (board == null) {
            throw new BizException("版块不存在");
        }
        int rank = rankOf(user);
        if (!canReadBoard(board, rank)) {
            throw new BizException(403, "无权在该版块发帖 · " + lockHint(board, rank));
        }
        if (!canPostOn(board, rank)) {
            throw new BizException(board.getReadOnly() != null && board.getReadOnly() == 1
                    ? "该版块为只读公告区，仅教员可发"
                    : "你的在校身份暂不满足本版块发帖要求");
        }

        int maxLevel = SecurityLevel.maxLevelOfCurrentUser();
        int level = SecurityLevel.normalize(dto.getSecurityLevel());
        if (level > maxLevel) {
            throw new BizException("不能发布高于自身可读密级的内容（当前上限："
                    + SecurityLevel.label(maxLevel) + "）");
        }

        checkInterval(threadMapper.lastPostAt(user.getId()), POST_INTERVAL_SECONDS,
                "发帖过于频繁，请稍后再试");

        String title = requireText(dto.getTitle(), 4, 80, "标题需 4–80 字");
        String content = requireText(dto.getContent(), 10, 8000, "正文需 10–8000 字");
        checkBanned(title);
        checkBanned(content);

        DrThread row = new DrThread();
        row.setBoardId(board.getId());
        row.setAuthorId(user.getId());
        row.setAuthorName(safe(user.getCodename()));
        row.setAuthorIdentity(safe(user.getIdentity()));
        row.setTitle(title);
        row.setContent(content);
        row.setSecurityLevel(level);
        row.setReplyVisible(Boolean.TRUE.equals(dto.getReplyVisible()) ? 1 : 0);
        row.setPinned(0);
        row.setEssence(0);
        row.setViewCount(0);
        row.setReplyCount(0);
        row.setLikeCount(0);
        row.setHeat(0);
        row.setStatus(1);
        row.setDeleted(0);
        row.setLastReplyAt(LocalDateTime.now());
        threadMapper.insert(row);

        DrThread saved = threadMapper.selectLive(row.getId());
        if (saved == null) {
            throw new BizException("发帖失败，请重试");
        }
        ThreadDetailVO out = new ThreadDetailVO();
        out.setMaxLevel(maxLevel);
        out.setViewerStaff(isStaff(user));
        out.setThread(toThreadVO(saved, board, user, maxLevel, false, false));
        out.setContent(saved.getContent());
        out.setCanReply(true);
        out.setReplyUnlocked(true);
        out.setReplies(new ArrayList<>());
        return out;
    }

    // ==================================================================
    // 回帖
    // ==================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ThreadDetailVO reply(Long threadId, ReplyCreateDTO dto) {
        DrUser user = requireLogin();
        DrThread row = threadMapper.selectLive(threadId);
        if (row == null) {
            throw new BizException("主题不存在或已下线");
        }
        DrBoard board = boardMapper.selectById(row.getBoardId());
        int rank = rankOf(user);
        if (board == null || !canReadBoard(board, rank)) {
            throw new BizException(403, "无权在该主题下回复");
        }
        int maxLevel = SecurityLevel.maxLevelOfCurrentUser();
        if (row.getSecurityLevel() != null && row.getSecurityLevel() > maxLevel) {
            throw new BizException(403, "密级不足，无法回复该主题");
        }

        checkInterval(replyMapper.lastReplyAt(user.getId()), REPLY_INTERVAL_SECONDS,
                "回复太快了，请稍等几秒");

        String content = requireText(dto.getContent(), 2, 2000, "回复需 2–2000 字");
        checkBanned(content);

        Long quoteId = dto.getQuoteId() == null ? 0L : dto.getQuoteId();
        if (quoteId > 0) {
            DrReply quoted = replyMapper.selectById(quoteId);
            if (quoted == null || !threadId.equals(quoted.getThreadId())) {
                throw new BizException("引用的回复不存在");
            }
        }

        Integer maxFloor = replyMapper.maxFloor(threadId);
        int floor = (maxFloor == null ? 0 : maxFloor) + 1;

        DrReply reply = new DrReply();
        reply.setThreadId(threadId);
        reply.setAuthorId(user.getId());
        reply.setAuthorName(safe(user.getCodename()));
        reply.setAuthorIdentity(safe(user.getIdentity()));
        reply.setContent(content);
        reply.setQuoteId(quoteId);
        reply.setFloorNo(floor);
        reply.setStatus(1);
        reply.setDeleted(0);
        replyMapper.insert(reply);

        long count = nz(replyMapper.countByThread(threadId));
        threadMapper.updateAfterReply(threadId, (int) count, reply.getCreateTime() == null
                ? LocalDateTime.now() : reply.getCreateTime());

        return detailInternal(threadId);
    }

    // ==================================================================
    // 编辑 / 删除
    // ==================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ThreadDetailVO update(Long id, ThreadUpdateDTO dto) {
        DrUser user = requireLogin();
        DrThread row = threadMapper.selectLive(id);
        if (row == null) {
            throw new BizException("主题不存在或已下线");
        }
        // 只认作者本人：教员事后改写别人的话等于篡改证据，要处理请走删除 / 举报下架
        if (row.getAuthorId() == null || !row.getAuthorId().equals(user.getId())) {
            throw new BizException(403, "只能编辑自己发布的主题");
        }

        String title = requireText(dto.getTitle(), 4, 80, "标题需 4–80 字");
        String content = requireText(dto.getContent(), 10, 8000, "正文需 10–8000 字");
        checkBanned(title + " " + content);

        int maxLevel = SecurityLevel.maxLevelOfCurrentUser();
        if (dto.getSecurityLevel() != null) {
            int level = SecurityLevel.normalize(dto.getSecurityLevel());
            if (level > maxLevel) {
                throw new BizException("发布密级不能高于你自己的可读上限");
            }
            row.setSecurityLevel(level);
        }

        row.setTitle(title);
        row.setContent(content);
        if (dto.getReplyVisible() != null) {
            row.setReplyVisible(dto.getReplyVisible() ? 1 : 0);
        }
        // 编辑不动楼层与热度，只更新时间戳 ——「已编辑」标记就是靠它与 create_time 比较得来的
        row.setUpdateTime(LocalDateTime.now());
        threadMapper.updateById(row);

        return detailInternal(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteThread(Long id, String reason) {
        DrUser user = requireLogin();
        DrThread row = threadMapper.selectLive(id);
        if (row == null) {
            throw new BizException("主题不存在或已下线");
        }
        boolean mine = row.getAuthorId() != null && row.getAuthorId().equals(user.getId());
        // 版务可以删任何帖（违规处置），普通读者只能删自己的
        if (!mine && !isStaff(user)) {
            throw new BizException(403, "只能删除自己发布的主题");
        }

        // deleted 带 @TableLogic，updateById 会静默忽略这个字段（删除会「假成功」），
        // 逻辑删除必须走 deleteById，它才会生成 SET deleted = 1 的语句
        threadMapper.deleteById(row.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReplyVO updateReply(Long threadId, Long replyId, ReplyUpdateDTO dto) {
        DrUser user = requireLogin();
        DrReply reply = replyMapper.selectById(replyId);
        if (reply == null || reply.getDeleted() != null && reply.getDeleted() == 1) {
            throw new BizException("该回复不存在或已删除");
        }
        if (!reply.getThreadId().equals(threadId)) {
            throw new BizException("回复与主题不匹配");
        }
        if (reply.getAuthorId() == null || !reply.getAuthorId().equals(user.getId())) {
            throw new BizException(403, "只能编辑自己发布的回复");
        }

        String content = requireText(dto.getContent(), 2, 2000, "回复需 2–2000 字");
        checkBanned(content);
        reply.setContent(content);
        reply.setUpdateTime(LocalDateTime.now());
        replyMapper.updateById(reply);

        return toReplyVOs(Collections.singletonList(reply), user).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReply(Long threadId, Long replyId) {
        DrUser user = requireLogin();
        DrReply reply = replyMapper.selectById(replyId);
        if (reply == null || reply.getDeleted() != null && reply.getDeleted() == 1) {
            throw new BizException("该回复不存在或已删除");
        }
        if (!reply.getThreadId().equals(threadId)) {
            throw new BizException("回复与主题不匹配");
        }
        boolean mine = reply.getAuthorId() != null && reply.getAuthorId().equals(user.getId());
        if (!mine && !isStaff(user)) {
            throw new BizException(403, "只能删除自己发布的回复");
        }

        // 同 deleteThread：逻辑删除只能走 deleteById
        replyMapper.deleteById(reply.getId());

        // 楼层数变了，主题上的冗余计数与热度必须跟着重算，否则列表上的数字对不上
        long count = nz(replyMapper.countByThread(threadId));
        DrThread t = threadMapper.selectById(threadId);
        threadMapper.updateAfterReply(threadId, (int) count,
                t == null || t.getLastReplyAt() == null ? LocalDateTime.now() : t.getLastReplyAt());
    }

    /** 是否具备版务身份（教员及以上）：能删别人的内容，但改不了别人的话 */
    private static boolean isStaff(DrUser user) {
        return user != null && identityRank(user.getIdentity()) >= identityRank("teacher");
    }

    // ==================================================================
    // 点赞
    // ==================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LikeVO like(Long threadId) {
        DrUser user = requireLogin();
        DrThread row = threadMapper.selectLive(threadId);
        if (row == null) {
            throw new BizException("主题不存在或已下线");
        }
        if (row.getSecurityLevel() != null
                && row.getSecurityLevel() > SecurityLevel.maxLevelOfCurrentUser()) {
            throw new BizException(403, "密级不足，无法点赞");
        }

        boolean liked;
        if (actionMapper.countByUser(threadId, user.getId(), DrThreadAction.LIKE) > 0) {
            actionMapper.deleteAction(threadId, user.getId(), DrThreadAction.LIKE);
            liked = false;
        } else {
            try {
                actionMapper.insert(new DrThreadAction(threadId, user.getId(), DrThreadAction.LIKE));
            } catch (DuplicateKeyException e) {
                // 并发双击：唯一键已经挡下第二条，按「已赞」处理即可
            }
            liked = true;
        }

        long count = nz(actionMapper.countByThread(threadId, DrThreadAction.LIKE));
        threadMapper.updateLikeCount(threadId, (int) count);
        return new LikeVO(liked, (int) count);
    }

    // ==================================================================
    // 收藏
    // ==================================================================

    /**
     * 收藏 / 取消收藏（同一接口切换）。
     *
     * <p>与点赞刻意分开：点赞影响热度（heat），收藏只影响「我的收藏夹」，
     * 混进同一个计数会让「收藏很多」的内容被热门算法奖赏 —— 那是错的权重。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectVO collect(Long threadId) {
        DrUser user = requireLogin();
        DrThread row = threadMapper.selectLive(threadId);
        if (row == null) {
            throw new BizException("主题不存在或已下线");
        }
        if (row.getSecurityLevel() != null
                && row.getSecurityLevel() > SecurityLevel.maxLevelOfCurrentUser()) {
            throw new BizException(403, "密级不足，无法收藏");
        }

        boolean collected;
        if (actionMapper.countByUser(threadId, user.getId(), DrThreadAction.COLLECT) > 0) {
            actionMapper.deleteAction(threadId, user.getId(), DrThreadAction.COLLECT);
            collected = false;
        } else {
            try {
                actionMapper.insert(new DrThreadAction(threadId, user.getId(), DrThreadAction.COLLECT));
            } catch (DuplicateKeyException e) {
                // 并发双击：唯一键已挡下第二条，按「已收藏」处理
            }
            collected = true;
        }
        return new CollectVO(collected);
    }

    @Override
    public PageVO<ThreadVO> collected(long page, long size) {
        DrUser user = requireLogin();
        int maxLevel = SecurityLevel.maxLevelOfCurrentUser();
        long p = Math.max(1L, page);
        long s = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        Long counted = threadMapper.countCollected(user.getId(), maxLevel);
        long total = counted == null ? 0L : counted;
        List<DrThread> rows = total == 0
                ? Collections.emptyList()
                : threadMapper.listCollected(user.getId(), maxLevel, (p - 1) * s, s);
        Map<Long, DrBoard> index = boardMapper.listOpen().stream()
                .collect(Collectors.toMap(DrBoard::getId, b -> b, (a, b) -> a));
        return new PageVO<>(toThreadVOs(rows, index, user, maxLevel), total, p, s);
    }

    // ==================================================================
    // 检索
    // ==================================================================

    @Override
    public List<ThreadVO> search(String keyword, int limit) {
        String kw = trimToNull(keyword);
        if (kw == null) {
            return Collections.emptyList();
        }
        if (kw.length() > 40) {
            kw = kw.substring(0, 40);
        }
        int max = Math.max(1, Math.min(limit, 20));
        int maxLevel = SecurityLevel.maxLevelOfCurrentUser();
        List<DrThread> rows = threadMapper.search(kw, maxLevel, max);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, DrBoard> index = boardMapper.listOpen().stream()
                .collect(Collectors.toMap(DrBoard::getId, b -> b, (a, b) -> a));
        return toThreadVOs(rows, index, CurrentUser.get(), maxLevel);
    }

    // ==================================================================
    // 内部工具
    // ==================================================================

    /** 回帖后重新组装详情，但不重复计浏览 */
    private ThreadDetailVO detailInternal(Long threadId) {
        DrThread row = threadMapper.selectLive(threadId);
        if (row == null) {
            throw new BizException("主题不存在或已下线");
        }
        DrUser user = CurrentUser.get();
        int maxLevel = SecurityLevel.maxLevelOfCurrentUser();
        DrBoard board = boardMapper.selectById(row.getBoardId());
        ThreadDetailVO out = new ThreadDetailVO();
        out.setMaxLevel(maxLevel);
        out.setViewerStaff(isStaff(user));
        out.setThread(toThreadVO(row, board, user, maxLevel,
                isLiked(threadId, user), isCollected(threadId, user)));
        out.setContent(row.getContent());
        // 回帖后落回「最后一页」：刚发的楼层在末尾，回到第 1 页会让用户以为没发出去
        Long counted = replyMapper.countByThread(threadId);
        long total = counted == null ? 0L : counted;
        long lastPage = total == 0 ? 1L : (total + REPLY_PAGE_SIZE - 1) / REPLY_PAGE_SIZE;
        PageVO<ReplyVO> tail = buildReplyPage(threadId, user, lastPage, REPLY_PAGE_SIZE);
        out.setReplies(tail.getList());
        out.setReplyTotal(tail.getTotal());
        out.setReplyPage(tail.getPage());
        out.setReplySize(tail.getSize());
        out.setReplyPages(tail.getPages());
        out.setCanReply(user != null);
        out.setReplyUnlocked(true);
        return out;
    }

    /**
     * 楼层分页：page 从 1 开始，size 默认 {@link #REPLY_PAGE_SIZE}、上限 {@link #REPLY_MAX_SIZE}。
     *
     * <p>为什么必须分页：原来 detail() 会把该主题所有楼层一次性返回，
     * 单主题到几百楼时响应体体积和前端渲染都会明显拖慢。</p>
     */
    private PageVO<ReplyVO> buildReplyPage(Long threadId, DrUser user, long page, long size) {
        long p = Math.max(1L, page);
        long s = size <= 0 ? REPLY_PAGE_SIZE : Math.min(size, REPLY_MAX_SIZE);
        Long counted = replyMapper.countByThread(threadId);
        long total = counted == null ? 0L : counted;
        List<DrReply> rows = total == 0
                ? Collections.emptyList()
                : replyMapper.listPageByThread(threadId, (p - 1) * s, s);
        return new PageVO<>(toReplyVOs(rows, user), total, p, s);
    }

    /**
     * 楼层实体 → VO。
     *
     * <p>引用（quoteId）指向的楼层可能不在当前页 —— 翻到第 5 页时引用第 2 页的楼层很常见。
     * 这种情况按 id 批量补一次，否则跨页引用会退化成一条没有上下文的回复。</p>
     */
    private List<ReplyVO> toReplyVOs(List<DrReply> rows, DrUser user) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, DrReply> index = new HashMap<>();
        for (DrReply r : rows) {
            index.put(r.getId(), r);
        }
        List<Long> missing = new ArrayList<>();
        for (DrReply r : rows) {
            Long qid = r.getQuoteId();
            if (qid != null && qid > 0 && !index.containsKey(qid)) {
                missing.add(qid);
            }
        }
        if (!missing.isEmpty()) {
            for (DrReply q : replyMapper.selectBatchIds(missing)) {
                index.put(q.getId(), q);
            }
        }

        List<ReplyVO> list = new ArrayList<>(rows.size());
        for (DrReply r : rows) {
            ReplyVO vo = new ReplyVO();
            vo.setId(r.getId());
            vo.setFloorNo(r.getFloorNo());
            vo.setAuthorId(r.getAuthorId());
            vo.setAuthorName(safe(r.getAuthorName()));
            vo.setAuthorIdentity(safe(r.getAuthorIdentity()));
            vo.setContent(r.getContent());
            vo.setCreatedAt(iso(r.getCreateTime()));
            vo.setMine(user != null && r.getAuthorId() != null && r.getAuthorId().equals(user.getId()));
            vo.setEdited(isEdited(r.getCreateTime(), r.getUpdateTime()));
            if (r.getQuoteId() != null && r.getQuoteId() > 0) {
                DrReply q = index.get(r.getQuoteId());
                if (q != null) {
                    vo.setQuoteFloor(q.getFloorNo());
                    vo.setQuoteAuthor(safe(q.getAuthorName()));
                    vo.setQuoteText(excerpt(q.getContent(), 60));
                }
            }
            list.add(vo);
        }
        return list;
    }

    private List<ThreadVO> toThreadVOs(List<DrThread> rows, Map<Long, DrBoard> index,
                                       DrUser user, int maxLevel) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> likedIds = likedIdsOf(rows, user);
        Set<Long> collectedIds = collectedIdsOf(rows, user);
        List<ThreadVO> list = new ArrayList<>(rows.size());
        for (DrThread row : rows) {
            list.add(toThreadVO(row, index.get(row.getBoardId()), user, maxLevel,
                    likedIds.contains(row.getId()), collectedIds.contains(row.getId())));
        }
        return list;
    }

    private ThreadVO toThreadVO(DrThread row, DrBoard board, DrUser user, int maxLevel,
                                boolean liked, boolean collected) {
        ThreadVO vo = new ThreadVO();
        vo.setId(row.getId());
        vo.setBoardId(row.getBoardId());
        vo.setBoardCode(board == null ? "" : safe(board.getCode()));
        vo.setBoardName(board == null ? "未知版块" : safe(board.getName()));
        vo.setTitle(safe(row.getTitle()));
        vo.setAuthorId(row.getAuthorId());
        vo.setAuthorName(safe(row.getAuthorName()));
        vo.setAuthorIdentity(safe(row.getAuthorIdentity()));
        vo.setSecurityLevel(row.getSecurityLevel());
        vo.setSecurityLabel(SecurityLevel.label(row.getSecurityLevel()));
        vo.setReplyVisible(row.getReplyVisible() != null && row.getReplyVisible() == 1);
        vo.setPinned(row.getPinned() != null && row.getPinned() == 1);
        vo.setEssence(row.getEssence() != null && row.getEssence() == 1);
        vo.setViewCount(nzInt(row.getViewCount()));
        vo.setReplyCount(nzInt(row.getReplyCount()));
        vo.setLikeCount(nzInt(row.getLikeCount()));
        vo.setHeat(nzInt(row.getHeat()));
        vo.setCreatedAt(iso(row.getCreateTime()));
        vo.setLastReplyAt(iso(row.getLastReplyAt()));
        vo.setMine(user != null && row.getAuthorId() != null && row.getAuthorId().equals(user.getId()));
        vo.setLiked(liked);
        vo.setCollected(collected);
        vo.setEdited(isEdited(row.getCreateTime(), row.getUpdateTime()));

        boolean levelOk = row.getSecurityLevel() == null || row.getSecurityLevel() <= maxLevel;
        vo.setLocked(!levelOk);
        boolean replyLocked = vo.isReplyVisible()
                && (user == null || !hasReplied(row.getId(), user.getId()));
        // 摘要也受权限约束：否则「密级不足」和「回复可见」都能从列表里读到内容
        vo.setExcerpt(levelOk && !replyLocked ? excerpt(row.getContent(), 140) : "");
        return vo;
    }

    private BoardVO toBoardVO(DrBoard b, int rank, int threadCount) {
        BoardVO vo = new BoardVO();
        vo.setId(b.getId());
        vo.setCode(safe(b.getCode()));
        vo.setName(safe(b.getName()));
        vo.setSubtitle(safe(b.getSubtitle()));
        vo.setDescription(safe(b.getDescription()));
        vo.setIcon(b.getIcon() == null || b.getIcon().isEmpty() ? "archive" : b.getIcon());
        vo.setThreadCount(threadCount);
        vo.setMinIdentity(safe(b.getMinIdentity()));
        vo.setReadOnly(b.getReadOnly() != null && b.getReadOnly() == 1);
        boolean readable = canReadBoard(b, rank);
        vo.setLocked(!readable);
        vo.setLockHint(readable ? "" : lockHint(b, rank));
        vo.setCanPost(readable && canPostOn(b, rank));
        return vo;
    }

    /**
     * 版块可见：状态开放 + 身份达标。
     *
     * <p>任务系统已下线，版块不再按「权限点」拦人 —— 权限点唯一的来源就是任务，
     * 保留这道门槛等于把所有受限版块永久锁死。现在只看在校身份
     * （dr_board.min_identity / post_min_identity）。</p>
     */
    private boolean canReadBoard(DrBoard board, int rank) {
        if (board.getStatus() == null || board.getStatus() != 1) {
            return false;
        }
        return rank >= identityRank(board.getMinIdentity());
    }

    /** 发帖权限：只读版仅教员；否则看 post_min_identity */
    private boolean canPostOn(DrBoard board, int rank) {
        if (rank <= 0) {
            return false;
        }
        if (board.getReadOnly() != null && board.getReadOnly() == 1) {
            return rank >= identityRank("teacher");
        }
        return rank >= identityRank(board.getPostMinIdentity());
    }

    /** 权限不足的原因说明（只剩身份一种原因） */
    private String lockHint(DrBoard board, int rank) {
        int needRank = identityRank(board.getMinIdentity());
        if (rank < needRank) {
            return "需" + identityLabel(board.getMinIdentity()) + "身份";
        }
        return "权限不足";
    }

    /** 在校身份 → 可比较的等级（与 SecurityLevel 的密级映射保持一致） */
    private static int identityRank(String identity) {
        if (identity == null) {
            return 0;
        }
        switch (identity) {
            case "teacher":
                return 3;
            case "executor":
                return 2;
            case "student":
            case "freshman":
                return 1;
            case "guest":
            default:
                return 0;
        }
    }

    private static String identityLabel(String identity) {
        if (identity == null) {
            return "更高";
        }
        switch (identity) {
            case "teacher":
                return "教员";
            case "executor":
                return "执行部";
            case "student":
                return "在校生";
            case "freshman":
                return "新生";
            default:
                return "更高";
        }
    }

    /** 当前登录者身份等级；被封印的账号按访客处理 */
    private static int rankOf(DrUser user) {
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
            return 0;
        }
        return identityRank(user.getIdentity());
    }

    private static String orderByOf(String sort) {
        if ("hot".equals(sort)) {
            return "t.pinned DESC, t.heat DESC, t.id DESC";
        }
        if ("essence".equals(sort)) {
            return "t.pinned DESC, t.essence DESC, COALESCE(t.last_reply_at, t.create_time) DESC, t.id DESC";
        }
        return "t.pinned DESC, COALESCE(t.last_reply_at, t.create_time) DESC, t.id DESC";
    }

    private boolean hasReplied(Long threadId, Long userId) {
        if (threadId == null || userId == null) {
            return false;
        }
        return replyMapper.countByAuthorInThread(threadId, userId) > 0;
    }

    private boolean isLiked(Long threadId, DrUser user) {
        if (threadId == null || user == null) {
            return false;
        }
        return actionMapper.countByUser(threadId, user.getId(), DrThreadAction.LIKE) > 0;
    }

    /** 批量回显「已赞」，避免列表里逐条查库 */
    private Set<Long> likedIdsOf(List<DrThread> rows, DrUser user) {
        if (user == null) {
            return Collections.emptySet();
        }
        List<Long> ids = rows.stream().map(DrThread::getId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(actionMapper.listLikedThreadIds(user.getId(), ids));
    }

    private boolean isCollected(Long threadId, DrUser user) {
        if (threadId == null || user == null) {
            return false;
        }
        return actionMapper.countByUser(threadId, user.getId(), DrThreadAction.COLLECT) > 0;
    }

    /** 批量回显「已收藏」 */
    private Set<Long> collectedIdsOf(List<DrThread> rows, DrUser user) {
        if (user == null) {
            return Collections.emptySet();
        }
        List<Long> ids = rows.stream().map(DrThread::getId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(actionMapper.listCollectedThreadIds(user.getId(), ids));
    }

    private DrUser requireLogin() {
        DrUser user = CurrentUser.get();
        if (user == null || user.getId() == null) {
            throw new BizException(401, "请先完成身份认证");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BizException(403, "该身份卡已被封印");
        }
        return user;
    }

    /** 频率限制：last 距今不足 seconds 秒则拒绝 */
    private static void checkInterval(LocalDateTime last, long seconds, String message) {
        if (last == null) {
            return;
        }
        long gap = Duration.between(last, LocalDateTime.now()).getSeconds();
        if (gap >= 0 && gap < seconds) {
            throw new BizException(message + "（间隔 " + seconds + " 秒）");
        }
    }

    private static String requireText(String text, int min, int max, String message) {
        String v = text == null ? "" : text.trim();
        if (v.length() < min || v.length() > max) {
            throw new BizException(message);
        }
        return v;
    }

    private static void checkBanned(String text) {
        for (String w : BANNED_WORDS) {
            if (text.contains(w)) {
                throw new BizException("内容包含不允许的词汇：" + w);
            }
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private static String excerpt(String content, int limit) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        String flat = content.replaceAll("\\s+", " ").trim();
        if (flat.length() <= limit) {
            return flat;
        }
        int end = limit;
        // 不要把代理对（emoji）截成半个字符
        if (Character.isHighSurrogate(flat.charAt(end - 1))) {
            end--;
        }
        return flat.substring(0, end) + "…";
    }

    private static String iso(LocalDateTime time) {
        return time == null ? null : ISO.format(time);
    }

    /**
     * 是否编辑过：update_time 晚于 create_time。
     *
     * <p>不加 edited 列是刻意的 —— 浏览数、楼层数、点赞数的自增都是手写 SQL，
     * 不会碰 update_time，所以这个比较不会因为「有人回帖」就被误判成编辑。</p>
     */
    private static boolean isEdited(LocalDateTime createTime, LocalDateTime updateTime) {
        return createTime != null && updateTime != null && updateTime.isAfter(createTime);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    private static int nzInt(Integer v) {
        return v == null ? 0 : v;
    }

    private static Map<Long, Integer> countMap(List<Map<String, Object>> rows) {
        Map<Long, Integer> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Object id = row.get("boardId");
            Object total = row.get("total");
            if (id instanceof Number && total instanceof Number) {
                map.put(((Number) id).longValue(), ((Number) total).intValue());
            }
        }
        return map;
    }
}
