package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrSearchLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 检索日志 Mapper。
 *
 * @author fenglin
 */
@Mapper
public interface DrSearchLogMapper extends BaseMapper<DrSearchLog> {

    /** 累计检索次数（首页统计面板用） */
    @Select("SELECT COUNT(*) FROM dr_search_log")
    Long countAll();
}
