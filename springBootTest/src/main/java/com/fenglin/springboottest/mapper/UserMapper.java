package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper。
 *
 * <p>继承 {@link BaseMapper} 后，增删改查、条件构造器（LambdaQueryWrapper）全部由
 * MyBatis-Plus 提供，无需再写 XML。下面只补充两个自定义查询作为示例。</p>
 *
 * @author fenglin
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 按骑士名查用户（示例：注解方式写 SQL）。
     * 逻辑删除条件由 MyBatis-Plus 自动追加。
     */
    @Select("SELECT * FROM kr_user WHERE username = #{username} LIMIT 1")
    User selectByUsername(@Param("username") String username);

    /**
     * 统计某个骑士名是否已存在（示例：返回基本类型）。
     */
    @Select("SELECT COUNT(1) FROM kr_user WHERE username = #{username} AND deleted = 0")
    int countByUsername(@Param("username") String username);

    /**
     * 统计某个邮箱是否已存在。
     */
    @Select("SELECT COUNT(1) FROM kr_user WHERE email = #{email} AND deleted = 0")
    int countByEmail(@Param("email") String email);
}
