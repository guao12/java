package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrAdmin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员表 Mapper。
 *
 * @author fenglin
 */
@Mapper
public interface DrAdminMapper extends BaseMapper<DrAdmin> {
}
