package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrMessage;
import com.fenglin.springboottest.vo.MessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 私信 Mapper。
 *
 * <p>会话查询一律带 {@code (sender, receiver)} 双向条件，索引
 * {@code idx_pair(sender_id, receiver_id, id)} 正好覆盖。</p>
 *
 * @author fenglin
 */
@Mapper
public interface DrMessageMapper extends BaseMapper<DrMessage> {

    /**
     * 某段会话的历史消息（按 id 倒序取一页，调用方再翻转成正序展示）。
     *
     * @param beforeId 游标：只取比它更早的消息，翻页用；首页传 Long.MAX_VALUE
     */
    @Select("SELECT m.id, m.sender_id, m.receiver_id, m.content, "
            + "DATE_FORMAT(m.create_time, '%Y-%m-%dT%H:%i:%s') AS created_at "
            + "FROM dr_message m "
            + "WHERE ((m.sender_id = #{me} AND m.receiver_id = #{friendId}) "
            + "    OR (m.sender_id = #{friendId} AND m.receiver_id = #{me})) "
            + "  AND m.id < #{beforeId} "
            + "ORDER BY m.id DESC LIMIT #{size}")
    List<MessageVO> listHistory(@Param("me") Long me,
                                @Param("friendId") Long friendId,
                                @Param("beforeId") Long beforeId,
                                @Param("size") int size);

    /**
     * 增量拉取：只取比 afterId 新的消息（前端轮询用）。
     *
     * <p>没有它就只能每次重拉一整页再整体替换，滚动位置和多页历史都会被冲掉。
     * 按 id 正序返回，前端直接 append。</p>
     */
    @Select("SELECT m.id, m.sender_id, m.receiver_id, m.content, "
            + "DATE_FORMAT(m.create_time, '%Y-%m-%dT%H:%i:%s') AS created_at "
            + "FROM dr_message m "
            + "WHERE ((m.sender_id = #{me} AND m.receiver_id = #{friendId}) "
            + "    OR (m.sender_id = #{friendId} AND m.receiver_id = #{me})) "
            + "  AND m.id > #{afterId} "
            + "ORDER BY m.id ASC LIMIT #{size}")
    List<MessageVO> listSince(@Param("me") Long me,
                              @Param("friendId") Long friendId,
                              @Param("afterId") Long afterId,
                              @Param("size") int size);

    /** 我的未读私信总数（导航角标 / 概览用） */    @Select("SELECT COUNT(*) FROM dr_message WHERE receiver_id = #{me} AND read_flag = 0")
    Long countUnread(@Param("me") Long me);

    /** 我与某人的未读条数 */
    @Select("SELECT COUNT(*) FROM dr_message "
            + "WHERE receiver_id = #{me} AND sender_id = #{friendId} AND read_flag = 0")
    Long countUnreadFrom(@Param("me") Long me, @Param("friendId") Long friendId);

    /** 打开会话即已读：把对方发给我的未读全部置 1 */
    @Update("UPDATE dr_message SET read_flag = 1 "
            + "WHERE receiver_id = #{me} AND sender_id = #{friendId} AND read_flag = 0")
    int markRead(@Param("me") Long me, @Param("friendId") Long friendId);

    /**
     * 距离我上次发消息过去了多少秒（没发过返回 null）。
     * 用于发送间隔限制，挡连点与脚本刷屏。
     */
    @Select("SELECT TIMESTAMPDIFF(SECOND, MAX(create_time), NOW()) FROM dr_message WHERE sender_id = #{me}")
    Long secondsSinceLastSend(@Param("me") Long me);
}
