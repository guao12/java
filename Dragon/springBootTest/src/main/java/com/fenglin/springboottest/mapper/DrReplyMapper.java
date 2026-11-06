package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrReply;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 论坛批注 Mapper。
 *
 * <p>批注按楼层升序取，数量天然有限（单主题一般几十条），不做分页；
 * 真要出现超长主题，再按 floor_no 分段即可。</p>
 *
 * @author fenglin
 */
@Mapper
public interface DrReplyMapper extends BaseMapper<DrReply> {

    /** 某主题的全部有效批注，按楼层升序 */
    @Select("SELECT * FROM dr_reply WHERE thread_id = #{threadId} AND deleted = 0 AND status = 1 "
            + "ORDER BY floor_no ASC, id ASC")
    List<DrReply> listByThread(@Param("threadId") Long threadId);

    /** 某主题的有效批注数 */
    @Select("SELECT COUNT(*) FROM dr_reply WHERE thread_id = #{threadId} AND deleted = 0 AND status = 1")
    Long countByThread(@Param("threadId") Long threadId);

    /** 当前最大楼层号，没有批注时返回 0 */
    @Select("SELECT COALESCE(MAX(floor_no), 0) FROM dr_reply WHERE thread_id = #{threadId} AND deleted = 0")
    Integer maxFloor(@Param("threadId") Long threadId);

    /** 某用户在指定主题下的批注数（判断「回复可见」是否已解锁） */
    @Select("SELECT COUNT(*) FROM dr_reply WHERE thread_id = #{threadId} AND author_id = #{authorId} "
            + "AND deleted = 0 AND status = 1")
    Long countByAuthorInThread(@Param("threadId") Long threadId, @Param("authorId") Long authorId);

    /** 某用户批注总数 */
    @Select("SELECT COUNT(*) FROM dr_reply WHERE author_id = #{authorId} AND deleted = 0 AND status = 1")
    Long countByAuthor(@Param("authorId") Long authorId);

    /** 全站可见批注总数（首页统计，按主题密级过滤） */
    @Select("SELECT COUNT(*) FROM dr_reply r "
            + "JOIN dr_thread t ON t.id = r.thread_id "
            + "WHERE r.deleted = 0 AND r.status = 1 AND t.deleted = 0 AND t.status = 1 "
            + "AND t.security_level <= #{maxLevel}")
    Long countVisible(@Param("maxLevel") int maxLevel);

    /** 同一作者最近一次批注时间，用于回帖频率限制 */
    @Select("SELECT MAX(create_time) FROM dr_reply WHERE author_id = #{authorId} AND deleted = 0")
    LocalDateTime lastReplyAt(@Param("authorId") Long authorId);

    /** 物理删除某主题的全部批注（仅在删除主题时使用，正常走逻辑删除） */
    @Delete("DELETE FROM dr_reply WHERE thread_id = #{threadId}")
    void purgeByThread(@Param("threadId") Long threadId);
}
