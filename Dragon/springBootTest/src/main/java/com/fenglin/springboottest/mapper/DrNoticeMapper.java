package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 首页公告 Mapper。
 *
 * @author fenglin
 */
@Mapper
public interface DrNoticeMapper extends BaseMapper<DrNotice> {

    /**
     * 这里没有 {@code <script>}，因此直接写 {@code >=}。
     * 注意：MyBatis 只有把 SQL 包在 {@code <script>} 里才会解析 XML 转义，
     */
    @Select("SELECT * FROM dr_notice "
            + "WHERE deleted = 0 AND status = 1 AND (expire_at IS NULL OR expire_at >= NOW()) "
            + "ORDER BY pinned DESC, publish_at DESC LIMIT #{limit}")
    List<DrNotice> listPublished(@Param("limit") long limit);
}
