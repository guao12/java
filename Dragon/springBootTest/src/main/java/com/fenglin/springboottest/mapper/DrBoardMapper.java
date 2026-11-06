package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrBoard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 论坛版块 Mapper。
 *
 * <p>版块数量很少（个位数），一次全取后在 Service 里按身份 / 权限点过滤，
 * 比在 SQL 里拼两套条件更好维护。</p>
 *
 * @author fenglin
 */
@Mapper
public interface DrBoardMapper extends BaseMapper<DrBoard> {

    /** 全部开放版块，按 sort_no 升序 */
    @Select("SELECT * FROM dr_board WHERE status = 1 ORDER BY sort_no ASC, id ASC")
    List<DrBoard> listOpen();

    /** 按 code 取版块（关闭的也取出来，由 Service 决定拒绝理由） */
    @Select("SELECT * FROM dr_board WHERE code = #{code} LIMIT 1")
    DrBoard selectByCode(@Param("code") String code);
}
