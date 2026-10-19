package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrArchiveCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 档案分类 Mapper。
 *
 * @author fenglin
 */
@Mapper
public interface DrArchiveCategoryMapper extends BaseMapper<DrArchiveCategory> {

    /** 全量启用中的分类，按父级与排序号返回，Service 侧再组装成两级树 */
    @Select("SELECT * FROM dr_archive_category WHERE status = 1 ORDER BY parent_id ASC, sort_no ASC, id ASC")
    List<DrArchiveCategory> listAllEnabled();

    /** 按父级查二级分类 */
    @Select("SELECT * FROM dr_archive_category WHERE parent_id = #{parentId} AND status = 1 ORDER BY sort_no ASC, id ASC")
    List<DrArchiveCategory> listChildren(@Param("parentId") Long parentId);

    /** 启用中的分类总数 */
    @Select("SELECT COUNT(*) FROM dr_archive_category WHERE status = 1")
    Long countEnabled();
}
