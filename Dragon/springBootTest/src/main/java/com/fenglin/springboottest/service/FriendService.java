package com.fenglin.springboottest.service;

import com.fenglin.springboottest.dto.FriendRequestCreateDTO;
import com.fenglin.springboottest.dto.MessageSendDTO;
import com.fenglin.springboottest.vo.FriendBadgeVO;
import com.fenglin.springboottest.vo.FriendOverviewVO;
import com.fenglin.springboottest.vo.FriendRequestVO;
import com.fenglin.springboottest.vo.FriendVO;
import com.fenglin.springboottest.vo.MessageVO;
import com.fenglin.springboottest.vo.UserSearchVO;

import java.util.List;

/**
 * 好友与私信服务。
 *
 * <p>权限前提：本服务所有方法都要求已登录，当前用户从
 * {@code CurrentUser} 取；未登录由 JwtInterceptor 提前拦掉。</p>
 *
 * @author fenglin
 */
public interface FriendService {

    /** 好友页概览：计数 + 待处理申请 + 好友（按最近会话排序） */
    FriendOverviewVO overview();

    /** 导航角标：未读私信数 + 待处理申请数 */
    FriendBadgeVO badge();

    /**
     * 搜索可添加的人（按学号 / 代号模糊匹配，排除自己）。
     *
     * @param kw 关键词，少于 2 个字符直接返回空
     */
    List<UserSearchVO> search(String kw);

    /** 我的好友列表 */
    List<FriendVO> friends();

    /** 我收到的待处理申请 */
    List<FriendRequestVO> requests();

    /**
     * 发送好友申请。
     *
     * <p>若对方此前已向我申请，则直接视为互相同意，省掉一次来回。</p>
     *
     * @return 提示文案，直接展示给调用方
     */
    String sendRequest(FriendRequestCreateDTO body);

    /** 同意申请 */
    String accept(Long requestId);

    /** 拒绝申请 */
    String reject(Long requestId);

    /** 解除好友关系 */
    String remove(Long friendId);

    /**
     * 会话历史（按时间正序返回一页）。
     *
     * @param friendId 对方用户 id
     * @param beforeId 游标，取比它更早的消息；首页传 null
     * @param size     条数，1–100
     */
    List<MessageVO> messages(Long friendId, Long beforeId, int size);

    /**
     * 增量拉取新消息（轮询用）：只返回 id 比 afterId 大的消息，按时间正序。
     *
     * @param afterId 我手上最新一条的 id
     */
    List<MessageVO> newMessages(Long friendId, Long afterId);

    /** 发送私信，返回入库后的消息 */
    MessageVO send(MessageSendDTO body);

    /** 标记与某人的会话为已读，返回剩余未读数 */
    Integer markRead(Long friendId);
}
