package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.dto.FriendRequestCreateDTO;
import com.fenglin.springboottest.dto.MessageSendDTO;
import com.fenglin.springboottest.service.FriendService;
import com.fenglin.springboottest.vo.FriendBadgeVO;
import com.fenglin.springboottest.vo.FriendOverviewVO;
import com.fenglin.springboottest.vo.FriendRequestVO;
import com.fenglin.springboottest.vo.FriendVO;
import com.fenglin.springboottest.vo.MessageVO;
import com.fenglin.springboottest.vo.UserSearchVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 好友与私信接口（全部需要登录）。
 *
 * <pre>
 * GET    /api/friend/overview                  概览：计数 + 申请 + 好友列表
 * GET    /api/friend/badge                      导航角标：未读私信 / 待处理申请
 * GET    /api/friend/search?kw=                 搜人（学号 / 代号）
 * GET    /api/friend/list                       我的好友
 * GET    /api/friend/requests                   我收到的待处理申请
 * POST   /api/friend/requests                   发送申请
 * POST   /api/friend/requests/{id}/accept       同意申请
 * POST   /api/friend/requests/{id}/reject       拒绝申请
 * DELETE /api/friend/{userId}                   解除好友
 * GET    /api/friend/messages?friendId=&beforeId=&size=   会话历史
 * POST   /api/friend/messages                   发送私信
 * POST   /api/friend/read?friendId=             会话标记已读
 * </pre>
 *
 * <p>与论坛不同，这里没有"软鉴权"：/api/friend/** 全量挂在 {@code JwtInterceptor} 下，
 * 未登录直接 401。好友与私信天生只对本人有意义，没有游客场景。</p>
 *
 * @author fenglin
 */
@RestController
@RequestMapping("/api/friend")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    /** 好友页概览 */
    @GetMapping("/overview")
    public R<FriendOverviewVO> overview() {
        return R.ok(friendService.overview());
    }

    /** 导航角标（轮询用，返回体要小） */
    @GetMapping("/badge")
    public R<FriendBadgeVO> badge() {
        return R.ok(friendService.badge());
    }

    /** 搜人：按学号或行动代号 */
    @GetMapping("/search")
    public R<List<UserSearchVO>> search(@RequestParam("kw") String kw) {
        return R.ok(friendService.search(kw));
    }

    /** 我的好友 */
    @GetMapping("/list")
    public R<List<FriendVO>> friends() {
        return R.ok(friendService.friends());
    }

    /** 我收到的待处理申请 */
    @GetMapping("/requests")
    public R<List<FriendRequestVO>> requests() {
        return R.ok(friendService.requests());
    }

    /** 发送好友申请（返回的字符串为提示文案，前端可直接展示） */
    @PostMapping("/requests")
    public R<String> sendRequest(@Valid @RequestBody FriendRequestCreateDTO body) {
        return R.ok(friendService.sendRequest(body));
    }

    /** 同意申请 */
    @PostMapping("/requests/{id}/accept")
    public R<String> accept(@PathVariable("id") Long id) {
        return R.ok(friendService.accept(id));
    }

    /** 拒绝申请 */
    @PostMapping("/requests/{id}/reject")
    public R<String> reject(@PathVariable("id") Long id) {
        return R.ok(friendService.reject(id));
    }

    /** 解除好友关系 */
    @DeleteMapping("/{userId}")
    public R<String> remove(@PathVariable("userId") Long userId) {
        return R.ok(friendService.remove(userId));
    }

    /**
     * 会话历史（时间正序）。
     *
     * @param beforeId 游标，翻更早的消息时传当前最旧一条的 id
     */
    @GetMapping("/messages")
    public R<List<MessageVO>> messages(@RequestParam("friendId") Long friendId,
                                       @RequestParam(value = "beforeId", required = false) Long beforeId,
                                       @RequestParam(value = "size", defaultValue = "30") int size) {
        return R.ok(friendService.messages(friendId, beforeId, size));
    }

    /**
     * 增量拉取新消息（轮询用）：返回 id 大于 afterId 的消息，时间正序。
     */
    @GetMapping("/messages/new")
    public R<List<MessageVO>> newMessages(@RequestParam("friendId") Long friendId,
                                          @RequestParam("afterId") Long afterId) {
        return R.ok(friendService.newMessages(friendId, afterId));
    }

    /** 发送私信 */
    @PostMapping("/messages")
    public R<MessageVO> send(@Valid @RequestBody MessageSendDTO body) {
        return R.ok(friendService.send(body));
    }

    /** 会话标记已读，返回标记后的未读总数 */
    @PostMapping("/read")
    public R<Integer> read(@RequestParam("friendId") Long friendId) {
        return R.ok(friendService.markRead(friendId));
    }
}
