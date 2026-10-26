package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 混血种（用户）Mapper。
 *
 * <p>继承 {@link BaseMapper} 后，增删改查、条件构造器（LambdaQueryWrapper）全部由
 * MyBatis-Plus 提供，无需写 XML。下面只补充两个注解 SQL 作为示例。</p>
 *
 * @author fenglin
 */
@Mapper
public interface DrUserMapper extends BaseMapper<DrUser> {

    /**
     * 按学院编号（学号）查用户。逻辑删除条件由 MyBatis-Plus 自动追加。
     */
    @Select("SELECT * FROM dr_user WHERE account = #{account} AND deleted = 0 LIMIT 1")
    DrUser selectByAccount(@Param("account") String account);

    /**
     * 按行动代号（姓名）查用户。codename 在注册时做了唯一校验，因此可唯一确定一位用户。
     * 逻辑删除条件由 MyBatis-Plus 自动追加。
     */
    @Select("SELECT * FROM dr_user WHERE codename = #{codename} AND deleted = 0 LIMIT 1")
    DrUser selectByCodename(@Param("codename") String codename);

    /**
     * 统计某年在册学号的最大序号，用于签发新学号（CC2026xxxxx）。
     */
    @Select("SELECT MAX(CAST(SUBSTRING(account, 7) AS UNSIGNED)) FROM dr_user WHERE account LIKE CONCAT('CC', #{year}, '%')")
    Integer maxAccountSeqOfYear(@Param("year") String year);
}
