package com.fenglin.springboottest.service.impl;

import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.common.CurrentUser;
import com.fenglin.springboottest.dto.FriendRequestCreateDTO;
import com.fenglin.springboottest.dto.MessageSendDTO;
import com.fenglin.springboottest.entity.DrFriendRequest;
import com.fenglin.springboottest.entity.DrMessage;
import com.fenglin.springboottest.entity.DrUser;
import com.fenglin.springboottest.mapper.DrFriendRequestMapper;
import com.fenglin.springboottest.mapper.DrFriendshipMapper;
import com.fenglin.springboottest.mapper.DrMessageMapper;
import com.fenglin.springboottest.mapper.DrUserMapper;
import com.fenglin.springboottest.service.DictService;
import com.fenglin.springboottest.service.FriendService;
import com.fenglin.springboottest.vo.FriendBadgeVO;
import com.fenglin.springboottest.vo.FriendOverviewVO;
import com.fenglin.springboottest.vo.FriendRequestVO;
import com.fenglin.springboottest.vo.FriendVO;
import com.fenglin.springboottest.vo.MessageVO;
import com.fenglin.springboottest.vo.UserSearchVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 好友与私信服务实现。
 *
 * <p>几条刻意的取舍：</p>
 * <ol>
 *   <li><b>单向关系、双向删除</b>：好友存两行（A→B、B→A），查得更快，
 *       但删除时一定两行同删 —— 只删一边会留下"我看得见他、他看不见我"的幽灵好友。</li>
 *   <li><b>申请可复用</b>：被拒后再申请不新建记录，把原记录状态重置为待处理，
 *       否则「待处理」里会堆出同一个人的多条申请。</li>
 *   <li><b>对方已申请则直接成为好友</b>：A 申请 B、B 又申请 A 时不再走一遍流程，
 *       两人的意图已经互明，多一步确认纯属添堵。</li>
 *   <li><b>只能给好友发私信</b>：私信一旦对非好友开放，就是第二个垃圾广告通道。</li>
 * </ol>
 *
 * @author fenglin
 */
@Service
public class FriendServiceImpl implements FriendService {

    /** 与前端展示约定一致：ISO 本地时间，不带时区后缀 */
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** 单条私信上限 */
    private static final int MAX_CONTENT = 500;
    /** 发送间隔：1 秒内只能发一条，挡连点 */
    private static final long SEND_INTERVAL_SECONDS = 1L;
    /** 搜索结果上限 */
    private static final int SEARCH_LIMIT = 10;
    /** 概览里返回的好友 / 申请条数上限 */
    private static final int OVERVIEW_LIMIT = 20;
    /** 会话一页默认 / 上限 */
    private static final int MESSAGE_PAGE_SIZE = 30;
    private static final int MESSAGE_MAX_SIZE = 100;

    private final DrFriendshipMapper friendshipMapper;
    private final DrFriendRequestMapper requestMapper;
    private final DrMessageMapper messageMapper;
    private final DrUserMapper userMapper;
    private final DictService dictService;

    public FriendServiceImpl(DrFriendshipMapper friendshipMapper,
                             DrFriendRequestMapper requestMapper,
                             DrMessageMapper messageMapper,
                             DrUserMapper userMapper,
                             DictService dictService) {
        this.friendshipMapper = friendshipMapper;
        this.requestMapper = requestMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.dictService = dictService;
    }

    // ==================================================================
    // 概览 / 角标
    // ==================================================================

    @Override
    public FriendOverviewVO overview() {
        Long me = me().getId();

        FriendOverviewVO vo = new FriendOverviewVO();
        vo.setFriendCount(nz(friendshipMapper.countFriends(me)));
        vo.setPendingCount(nz(requestMapper.countIncoming(me)));
        vo.setUnreadCount(nz(messageMapper.countUnread(me)));

        vo.setRequests(limit(fillRequestNames(requestMapper.listIncoming(me)), OVERVIEW_LIMIT));
        vo.setFriends(limit(fillFriendNames(friendshipMapper.listFriends(me)), OVERVIEW_LIMIT));
        return vo;
    }

    @Override
    public FriendBadgeVO badge() {
        Long me = me().getId();
        return new FriendBadgeVO(nz(messageMapper.countUnread(me)), nz(requestMapper.countIncoming(me)));
    }

    // ==================================================================
    // 搜索 / 好友列表 / 申请列表
    // ==================================================================

    @Override
    public List<UserSearchVO> search(String kw) {
        Long me = me().getId();
        String q = kw == null ? "" : kw.trim();
        // 空串会让 LIKE '%%' 扫全表，直接挡掉
        if (q.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserSearchVO> rows = userMapper.searchForFriend(me, q, SEARCH_LIMIT);
        if (rows.isEmpty()) {
            return rows;
        }

        // 三个集合一次取齐，避免在循环里逐个查库（N+1）
        Set<Long> friendIds = new HashSet<>(friendshipMapper.listFriendIds(me));
        Set<Long> pendingOut = new HashSet<>(requestMapper.listPendingOutIds(me));
        Set<Long> pendingIn = new HashSet<>(requestMapper.listPendingInIds(me));

        for (UserSearchVO u : rows) {
            u.setIdentityName(dictService.identityName(u.getIdentity()));
            u.setSpiritName(dictService.spiritName(u.getSpirit()));
            if (friendIds.contains(u.getId())) {
                u.setRelation("friend");
                u.setRelationLabel("已是好友");
            } else if (pendingOut.contains(u.getId())) {
                u.setRelation("pending_out");
                u.setRelationLabel("已申请");
            } else if (pendingIn.contains(u.getId())) {
                u.setRelation("pending_in");
                u.setRelationLabel("待你处理");
            } else {
                u.setRelation("none");
                u.setRelationLabel("");
            }
        }
        return rows;
    }

    @Override
    public List<FriendVO> friends() {
        return fillFriendNames(friendshipMapper.listFriends(me().getId()));
    }

    @Override
    public List<FriendRequestVO> requests() {
        return fillRequestNames(requestMapper.listIncoming(me().getId()));
    }

    // ==================================================================
    // 申请：发 / 同意 / 拒绝
    // ==================================================================

    @Override
    @Transactional
    public String sendRequest(FriendRequestCreateDTO body) {
        DrUser me = me();
        Long to = body.getToUserId();
        if (to == null || to.equals(me.getId())) {
            throw new BizException("不能添加自己为好友");
        }

        DrUser target = userMapper.selectById(to);
        if (target == null) {
            throw new BizException("该学号不存在");
        }
        if (target.getStatus() != null && target.getStatus() == 0) {
            throw new BizException("该账号已被封印，无法添加");
        }
        if (nz(friendshipMapper.countPair(me.getId(), to)) > 0) {
            throw new BizException("你们已经是好友了");
        }

        // 对方先发过申请且还没处理 —— 两人的意图已经互明，直接结为好友
        DrFriendRequest reverse = requestMapper.findPair(to, me.getId());
        if (reverse != null && reverse.getStatus() != null
                && reverse.getStatus() == DrFriendRequest.STATUS_PENDING) {
            friendshipMapper.insertBoth(me.getId(), to);
            requestMapper.resolvePair(me.getId(), to, DrFriendRequest.STATUS_ACCEPTED);
            return "对方此前已向你发出申请，已直接成为好友";
        }

        String message = body.getMessage() == null ? "" : body.getMessage().trim();
        requestMapper.upsert(me.getId(), to, message);
        return "申请已送达，等待对方确认";
    }

    @Override
    @Transactional
    public String accept(Long requestId) {
        Long me = me().getId();
        DrFriendRequest req = requestMapper.selectById(requestId);
        if (req == null || req.getToUserId() == null || !req.getToUserId().equals(me)) {
            throw new BizException("申请不存在");
        }
        if (req.getStatus() == null || req.getStatus() != DrFriendRequest.STATUS_PENDING) {
            throw new BizException("该申请已处理过了");
        }
        if (nz(friendshipMapper.countPair(me, req.getFromUserId())) > 0) {
            // 已经是好友：只把申请结案，不重复插入关系
            requestMapper.resolvePair(me, req.getFromUserId(), DrFriendRequest.STATUS_ACCEPTED);
            return "你们已经是好友了";
        }

        friendshipMapper.insertBoth(me, req.getFromUserId());
        requestMapper.resolvePair(me, req.getFromUserId(), DrFriendRequest.STATUS_ACCEPTED);
        return "已添加为好友";
    }

    @Override
    public String reject(Long requestId) {
        Long me = me().getId();
        int rows = requestMapper.handle(requestId, me, DrFriendRequest.STATUS_REJECTED);
        if (rows == 0) {
            throw new BizException("该申请不存在或已处理");
        }
        return "已拒绝该申请";
    }

    @Override
    @Transactional
    public String remove(Long friendId) {
        Long me = me().getId();
        if (friendId == null || nz(friendshipMapper.countPair(me, friendId)) == 0) {
            throw new BizException("你们还不是好友");
        }
        // 双向两行一起删；历史私信保留，重新加回好友后还能看到
        friendshipMapper.deleteBoth(me, friendId);
        return "已解除好友关系";
    }

    // ==================================================================
    // 私信
    // ==================================================================

    @Override
    public List<MessageVO> messages(Long friendId, Long beforeId, int size) {
        Long me = me().getId();
        requireFriend(me, friendId);

        int limit = size <= 0 ? MESSAGE_PAGE_SIZE : Math.min(size, MESSAGE_MAX_SIZE);
        long cursor = beforeId == null || beforeId <= 0 ? Long.MAX_VALUE : beforeId;

        List<MessageVO> rows = messageMapper.listHistory(me, friendId, cursor, limit);
        // 倒序取的一页，翻转成正序给前端直接渲染
        Collections.reverse(rows);
        for (MessageVO m : rows) {
            m.setMine(m.getSenderId() != null && m.getSenderId().equals(me));
        }

        // 拉到消息即视为已读：用户已经看到这一屏了
        messageMapper.markRead(me, friendId);
        return rows;
    }

    @Override
    public List<MessageVO> newMessages(Long friendId, Long afterId) {
        Long me = me().getId();
        requireFriend(me, friendId);
        if (afterId == null || afterId < 0) {
            return Collections.emptyList();
        }

        List<MessageVO> rows = messageMapper.listSince(me, friendId, afterId, MESSAGE_MAX_SIZE);
        for (MessageVO m : rows) {
            m.setMine(m.getSenderId() != null && m.getSenderId().equals(me));
        }
        if (!rows.isEmpty()) {
            // 轮询期间收到的新消息也一并清掉未读
            messageMapper.markRead(me, friendId);
        }
        return rows;
    }

    @Override
    public MessageVO send(MessageSendDTO body) {
        Long me = me().getId();
        Long to = body.getToUserId();
        requireFriend(me, to);

        String content = body.getContent() == null ? "" : body.getContent().trim();
        if (content.isEmpty()) {
            throw new BizException("消息不能为空");
        }
        if (content.length() > MAX_CONTENT) {
            throw new BizException("单条消息最多 " + MAX_CONTENT + " 字");
        }

        // 服务端节流：前端禁用按钮挡不住脚本，这里再兜一层
        Long gap = messageMapper.secondsSinceLastSend(me);
        if (gap != null && gap < SEND_INTERVAL_SECONDS) {
            throw new BizException("发送得太快了，稍等一下");
        }

        DrMessage msg = new DrMessage(me, to, content);
        messageMapper.insert(msg);

        MessageVO vo = new MessageVO();
        vo.setId(msg.getId());
        vo.setSenderId(me);
        vo.setReceiverId(to);
        vo.setContent(content);
        vo.setMine(true);
        vo.setCreatedAt(ISO.format(LocalDateTime.now()));
        return vo;
    }

    @Override
    public Integer markRead(Long friendId) {
        Long me = me().getId();
        messageMapper.markRead(me, friendId);
        return nz(messageMapper.countUnread(me));
    }

    // ==================================================================
    // 内部工具
    // ==================================================================

    /** 当前登录用户；理论上拦截器已保证非空，这里只做兜底 */
    private DrUser me() {
        DrUser user = CurrentUser.get();
        if (user == null) {
            throw new BizException(401, "登录状态已失效，请重新认证");
        }
        return user;
    }

    /** 私信前置条件：必须是好友，且不能发给自己 */
    private void requireFriend(Long me, Long friendId) {
        if (friendId == null) {
            throw new BizException("缺少会话对象");
        }
        if (friendId.equals(me)) {
            throw new BizException("不能和自己私信");
        }
        if (nz(friendshipMapper.countPair(me, friendId)) == 0) {
            throw new BizException("你们还不是好友，先加好友再聊");
        }
    }

    /** 补身份 / 言灵中文名（库里存的是 code） */
    private List<FriendVO> fillFriendNames(List<FriendVO> rows) {
        for (FriendVO f : rows) {
            f.setIdentityName(dictService.identityName(f.getIdentity()));
            f.setSpiritName(dictService.spiritName(f.getSpirit()));
            if (f.getUnread() == null) {
                f.setUnread(0);
            }
        }
        return rows;
    }

    private List<FriendRequestVO> fillRequestNames(List<FriendRequestVO> rows) {
        for (FriendRequestVO r : rows) {
            r.setIdentityName(dictService.identityName(r.getIdentity()));
        }
        return rows;
    }

    /** 只取前 n 条（好友很多时概览接口不该把整张表拖下来） */
    private <T> List<T> limit(List<T> rows, int max) {
        if (rows == null) {
            return new ArrayList<>();
        }
        return rows.size() <= max ? rows : new ArrayList<>(rows.subList(0, max));
    }

    private int nz(Long v) {
        return v == null ? 0 : v.intValue();
    }
}
