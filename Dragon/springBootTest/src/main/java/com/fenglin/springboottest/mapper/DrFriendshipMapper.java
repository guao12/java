package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrFriendship;
import com.fenglin.springboottest.vo.FriendVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 好友关系 Mapper（dr_friendship，双向各一行）。
 *
 * <p>列表查询用相关子查询直接带出「最后一条消息 / 最后时间 / 未读数」，
 * 一条 SQL 就能渲染整个好友列表，不用前端再逐个会话发请求。
 * 会话数据量小（一对一私聊）时，这是最省事的写法。</p>
 *
 * @author fenglin
 */
@Mapper
public interface DrFriendshipMapper extends BaseMapper<DrFriendship> {

    /** 我是否已加他为好友（1 = 是） */
    @Select("SELECT COUNT(*) FROM dr_friendship WHERE user_id = #{userId} AND friend_id = #{friendId}")
    Long countPair(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /** 我的好友总数 */
    @Select("SELECT COUNT(*) FROM dr_friendship f JOIN dr_user u ON u.id = f.friend_id AND u.deleted = 0 "
            + "WHERE f.user_id = #{userId}")
    Long countFriends(@Param("userId") Long userId);

    /** 我的好友 id 集合（搜索结果里判断「已是好友」用） */
    @Select("SELECT friend_id FROM dr_friendship WHERE user_id = #{userId}")
    List<Long> listFriendIds(@Param("userId") Long userId);

    /**
     * 好友列表（带最后一条消息、未读数），有聊天的排前面。
     *
     * <p>排序用 {@code last_at IS NULL} 打头：没聊过的人沉底，
     * 再按时间倒序，最后按成为好友的时间兜底，避免列表顺序抖动。</p>
     */
    @Select("SELECT u.id, u.account, u.codename, u.identity, u.spirit, "
            + "DATE_FORMAT(f.create_time, '%Y-%m-%dT%H:%i:%s') AS since, "
            + "(SELECT m.content FROM dr_message m "
            + "   WHERE (m.sender_id = u.id AND m.receiver_id = #{me}) "
            + "      OR (m.sender_id = #{me} AND m.receiver_id = u.id) "
            + "   ORDER BY m.id DESC LIMIT 1) AS last_message, "
            + "(SELECT DATE_FORMAT(m.create_time, '%Y-%m-%dT%H:%i:%s') FROM dr_message m "
            + "   WHERE (m.sender_id = u.id AND m.receiver_id = #{me}) "
            + "      OR (m.sender_id = #{me} AND m.receiver_id = u.id) "
            + "   ORDER BY m.id DESC LIMIT 1) AS last_at, "
            + "(SELECT COUNT(*) FROM dr_message m "
            + "   WHERE m.sender_id = u.id AND m.receiver_id = #{me} AND m.read_flag = 0) AS unread "
            + "FROM dr_friendship f "
            + "JOIN dr_user u ON u.id = f.friend_id AND u.deleted = 0 "
            + "WHERE f.user_id = #{me} "
            + "ORDER BY last_at IS NULL, last_at DESC, f.create_time DESC")
    List<FriendVO> listFriends(@Param("me") Long me);

    /**
     * 建立好友关系：一条语句写入双向两行。
     *
     * <p>IGNORE 兜底重复（先加好友后同意申请的竞态），不会报错。</p>
     */
    @Insert("INSERT IGNORE INTO dr_friendship (user_id, friend_id) "
            + "VALUES (#{a}, #{b}), (#{b}, #{a})")
    int insertBoth(@Param("a") Long a, @Param("b") Long b);

    /** 解除好友关系：双向两行一起删，否则会留下「单向好友」 */
    @Delete("DELETE FROM dr_friendship "
            + "WHERE (user_id = #{a} AND friend_id = #{b}) "
            + "   OR (user_id = #{b} AND friend_id = #{a})")
    int deleteBoth(@Param("a") Long a, @Param("b") Long b);
}
