package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrFriendRequest;
import com.fenglin.springboottest.vo.FriendRequestVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 好友申请 Mapper。
 *
 * <p>申请用 {@code ON DUPLICATE KEY UPDATE} 复用同一行：被拒后再申请只需把
 * status 置回待处理，不会在「待处理」列表里堆出同一个人的多条记录。</p>
 *
 * @author fenglin
 */
@Mapper
public interface DrFriendRequestMapper extends BaseMapper<DrFriendRequest> {

    /** 我收到的待处理申请（带申请人身份信息） */
    @Select("SELECT r.id, r.from_user_id, u.account, u.codename, u.identity, r.message, "
            + "DATE_FORMAT(r.create_time, '%Y-%m-%dT%H:%i:%s') AS created_at "
            + "FROM dr_friend_request r "
            + "JOIN dr_user u ON u.id = r.from_user_id AND u.deleted = 0 "
            + "WHERE r.to_user_id = #{me} AND r.status = 0 "
            + "ORDER BY r.id DESC")
    List<FriendRequestVO> listIncoming(@Param("me") Long me);

    /** 我收到的待处理申请数（导航角标用） */
    @Select("SELECT COUNT(*) FROM dr_friend_request WHERE to_user_id = #{me} AND status = 0")
    Long countIncoming(@Param("me") Long me);

    /** 我发出的、尚未被处理过的申请人 id（搜索结果回显「已申请」用） */
    @Select("SELECT to_user_id FROM dr_friend_request WHERE from_user_id = #{me} AND status = 0")
    List<Long> listPendingOutIds(@Param("me") Long me);

    /** 向我申请、我还没处理的申请人 id（搜索结果回显「待你处理」用） */
    @Select("SELECT from_user_id FROM dr_friend_request WHERE to_user_id = #{me} AND status = 0")
    List<Long> listPendingInIds(@Param("me") Long me);

    /** 查某个方向的申请记录（不论状态） */
    @Select("SELECT * FROM dr_friend_request WHERE from_user_id = #{from} AND to_user_id = #{to}")
    DrFriendRequest findPair(@Param("from") Long from, @Param("to") Long to);

    /** 新建 / 复用申请：重复申请把状态重置为待处理 */
    @Insert("INSERT INTO dr_friend_request (from_user_id, to_user_id, message, status) "
            + "VALUES (#{from}, #{to}, #{message}, 0) "
            + "ON DUPLICATE KEY UPDATE message = #{message}, status = 0, handled_at = NULL")
    int upsert(@Param("from") Long from, @Param("to") Long to, @Param("message") String message);

    /** 处理申请：同意 / 拒绝 */
    @Update("UPDATE dr_friend_request SET status = #{status}, handled_at = NOW() "
            + "WHERE id = #{id} AND to_user_id = #{me} AND status = 0")
    int handle(@Param("id") Long id, @Param("me") Long me, @Param("status") int status);

    /**
     * 把两人之间「所有方向」的待处理申请一并结案。
     *
     * <p>场景：A 申请 B，B 还没处理时 B 反过来申请 A —— 这时直接成为好友，
     * 但两条申请记录都还挂在"待处理"，不清掉会永远卡在角标里。
     * 用一条语句按 status = 0 过滤，已拒绝的历史记录不会被翻出来改。</p>
     */
    @Update("UPDATE dr_friend_request SET status = #{status}, handled_at = NOW() "
            + "WHERE ((from_user_id = #{a} AND to_user_id = #{b}) "
            + "    OR (from_user_id = #{b} AND to_user_id = #{a})) "
            + "  AND status = 0")
    int resolvePair(@Param("a") Long a, @Param("b") Long b, @Param("status") int status);
}
