package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrThreadAction;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 主题互动 Mapper（点赞 / 收藏 / 举报）。
 *
 * <p>写操作靠唯一键兜底：重复点赞会抛 DuplicateKeyException，
 * Service 捕获后按「已点过」处理，不需要先查再插。</p>
 *
 * @author fenglin
 */
@Mapper
public interface DrThreadActionMapper extends BaseMapper<DrThreadAction> {

    /** 某类互动的总数 */
    @Select("SELECT COUNT(*) FROM dr_thread_action WHERE thread_id = #{threadId} AND type = #{type}")
    Long countByThread(@Param("threadId") Long threadId, @Param("type") String type);

    /** 某人是否对某帖做过某类互动 */
    @Select("SELECT COUNT(*) FROM dr_thread_action "
            + "WHERE thread_id = #{threadId} AND user_id = #{userId} AND type = #{type}")
    Long countByUser(@Param("threadId") Long threadId,
                     @Param("userId") Long userId,
                     @Param("type") String type);

    /** 取消互动 */
    @Delete("DELETE FROM dr_thread_action "
            + "WHERE thread_id = #{threadId} AND user_id = #{userId} AND type = #{type}")
    int deleteAction(@Param("threadId") Long threadId,
                     @Param("userId") Long userId,
                     @Param("type") String type);

    /** 某人点赞过的主题 id 列表（列表页回显「已赞」） */
    @Select("<script>"
            + "SELECT thread_id FROM dr_thread_action "
            + "WHERE user_id = #{userId} AND type = 'like' "
            + "AND thread_id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<Long> listLikedThreadIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    /** 某人收藏过的主题 id 列表（列表页回显「已收藏」） */
    @Select("<script>"
            + "SELECT thread_id FROM dr_thread_action "
            + "WHERE user_id = #{userId} AND type = 'collect' "
            + "AND thread_id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<Long> listCollectedThreadIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}
