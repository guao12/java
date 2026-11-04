package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrUserClearance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户权限进度 Mapper。
 *
 * @author fenglin
 */
@Mapper
public interface DrUserClearanceMapper extends BaseMapper<DrUserClearance> {

    /** 按用户 id 查权限记录（user_id 唯一，故最多一条） */
    @Select("SELECT * FROM dr_user_clearance WHERE user_id = #{userId} LIMIT 1")
    DrUserClearance selectByUserId(@Param("userId") Long userId);
}
