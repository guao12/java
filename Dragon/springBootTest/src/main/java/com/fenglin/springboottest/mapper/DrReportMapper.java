package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 举报工单 Mapper。
 *
 * <p>按状态分页取工单；队列排序用 create_time 升序 —— 举报讲究先来先处理，
 * 让最早的那条一直沉在下面只会积累怨气。</p>
 *
 * @author fenglin
 */
@Mapper
public interface DrReportMapper extends BaseMapper<DrReport> {

    /** 某人是否已经举报过该对象（唯一键之外再查一次，便于给出友好提示） */
    @Select("SELECT COUNT(*) FROM dr_report "
            + "WHERE target_type = #{targetType} AND target_id = #{targetId} AND reporter_id = #{reporterId}")
    Long countByReporter(@Param("targetType") String targetType,
                         @Param("targetId") Long targetId,
                         @Param("reporterId") Long reporterId);

    /** 按状态分页取工单 */
    @Select("SELECT * FROM dr_report WHERE status = #{status} "
            + "ORDER BY create_time ASC, id ASC LIMIT #{size} OFFSET #{offset}")
    List<DrReport> listByStatus(@Param("status") int status,
                                @Param("offset") long offset,
                                @Param("size") long size);

    /** 全部分页（status = -1 时用），排序同队列 */
    @Select("SELECT * FROM dr_report ORDER BY status ASC, create_time ASC, id ASC "
            + "LIMIT #{size} OFFSET #{offset}")
    List<DrReport> selectPageList(@Param("offset") long offset, @Param("size") long size);

    /** 工单总数（不分状态） */
    @Select("SELECT COUNT(*) FROM dr_report")
    Long countAll();

    /** 同一对象上仍待处理的其他工单（处理一条时一并结案） */
    @Select("SELECT * FROM dr_report WHERE target_type = #{targetType} AND target_id = #{targetId} AND status = 0")
    List<DrReport> listPendingByTarget(@Param("targetType") String targetType,
                                       @Param("targetId") Long targetId);

    /** 某状态的工单数 */
    @Select("SELECT COUNT(*) FROM dr_report WHERE status = #{status}")
    Long countByStatus(@Param("status") int status);
}
