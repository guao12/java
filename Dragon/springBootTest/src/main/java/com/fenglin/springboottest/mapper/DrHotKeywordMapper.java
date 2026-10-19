package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrHotKeyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 热门检索词 Mapper。
 *
 * @author fenglin
 */
@Mapper
public interface DrHotKeywordMapper extends BaseMapper<DrHotKeyword> {

    /** 热词榜：先按运营配置的排序号，再按热度 */
    @Select("SELECT * FROM dr_hot_keyword WHERE status = 1 ORDER BY sort_no ASC, heat DESC LIMIT #{limit}")
    List<DrHotKeyword> listTop(@Param("limit") long limit);

    /** 命中已有热词时热度 +1（不自动收录新词，避免检索日志污染热词榜） */
    @Update("UPDATE dr_hot_keyword SET heat = heat + 1 WHERE keyword = #{keyword}")
    int bumpHeat(@Param("keyword") String keyword);
}
