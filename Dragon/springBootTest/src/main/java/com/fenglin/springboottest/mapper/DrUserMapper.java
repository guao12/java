package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrUser;
import com.fenglin.springboottest.vo.UserSearchVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
     * 按学院编号（学号）查用户。
     * 注意：这里是手写 SQL，MyBatis-Plus 不会自动追加逻辑删除条件，故显式带 deleted = 0。
     */
    @Select("SELECT * FROM dr_user WHERE account = #{account} AND deleted = 0 LIMIT 1")
    DrUser selectByAccount(@Param("account") String account);

    /**
     * 按行动代号（姓名）查用户。codename 在注册时做了唯一校验，因此可唯一确定一位用户。
     * 注意：这里是手写 SQL，MyBatis-Plus 不会自动追加逻辑删除条件，故显式带 deleted = 0。
     */
    @Select("SELECT * FROM dr_user WHERE codename = #{codename} AND deleted = 0 LIMIT 1")
    DrUser selectByCodename(@Param("codename") String codename);

    /**
     * 统计某年在册学号的最大序号，用于签发新学号（CC2026xxxxx）。
     */
    @Select("SELECT MAX(CAST(SUBSTRING(account, 7) AS UNSIGNED)) FROM dr_user WHERE account LIKE CONCAT('CC', #{year}, '%')")
    Integer maxAccountSeqOfYear(@Param("year") String year);

    /**
     * 好友模块：按学号 / 代号模糊搜索可添加的人。
     *
     * <p>只返回在册（status=1）、未删除、且不是自己的账号。
     * 排序让"完全等于关键词"的排在前面 —— 输入完整学号时第一行就是本人，
     * 不用在十条结果里挑。调用方负责补 relation（是否已是好友 / 有无待处理申请）。</p>
     */
    @Select("SELECT id, account, codename, identity, spirit FROM dr_user "
            + "WHERE deleted = 0 AND status = 1 AND id <> #{me} "
            + "  AND (account LIKE CONCAT('%', #{kw}, '%') OR codename LIKE CONCAT('%', #{kw}, '%')) "
            + "ORDER BY (account = #{kw}) DESC, (codename = #{kw}) DESC, id DESC "
            + "LIMIT #{limit}")
    List<UserSearchVO> searchForFriend(@Param("me") Long me,
                                      @Param("kw") String kw,
                                      @Param("limit") int limit);
}
