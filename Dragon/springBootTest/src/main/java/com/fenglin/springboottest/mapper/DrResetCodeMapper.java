package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrResetCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 密钥重置验证码 Mapper。
 *
 * <p>查询全部用 MyBatis-Plus 的 LambdaQueryWrapper 在 Service 层完成，无需 XML。</p>
 *
 * @author fenglin
 */
@Mapper
public interface DrResetCodeMapper extends BaseMapper<DrResetCode> {
}
