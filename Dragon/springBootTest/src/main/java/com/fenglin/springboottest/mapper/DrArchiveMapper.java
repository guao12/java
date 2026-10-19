package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrArchive;
import com.fenglin.springboottest.vo.FacetVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 档案 Mapper。
 *
 * <p>项目未引入 MyBatis-Plus 分页插件（jsqlparser），因此分页、分面统计
 * 全部用注解 SQL 手写：LIMIT 分页 + GROUP BY 统计。排序字段由 Service 侧白名单拼装，
 * 绝不接受前端直接传入的列名，避免 SQL 注入。</p>
 *
 * <p>所有查询都显式带上 {@code deleted = 0}、{@code status = 1}
 * 以及 {@code security_level <= maxLevel}（密级分级过滤）。</p>
 *
 * @author fenglin
 */
@Mapper
public interface DrArchiveMapper extends BaseMapper<DrArchive> {

    /** 关键词命中条件：卷宗编号 / 标题 / 摘要 / 标签 */
    String KW_COND = "(a.title LIKE CONCAT('%', #{kw}, '%') "
            + "OR a.summary LIKE CONCAT('%', #{kw}, '%') "
            + "OR a.tags LIKE CONCAT('%', #{kw}, '%') "
            + "OR a.code LIKE CONCAT('%', #{kw}, '%')) ";

    /** 基础过滤：未删除 + 已上架 + 密级在可见范围内 */
    String BASE_WHERE = "WHERE a.deleted = 0 AND a.status = 1 AND a.security_level &lt;= #{maxLevel} ";

    // ------------------------------------------------------------------
    // 检索：总数
    // ------------------------------------------------------------------

    /**
     * 统计命中总数（含关键词、两级分类、密级筛选）。
     */
    @Select("<script>"
            + "SELECT COUNT(*) FROM dr_archive a " + BASE_WHERE
            + "<if test='kw != null and kw != \"\"'>AND " + KW_COND + "</if>"
            + "<if test='topCategory != null'>AND a.top_category_id = #{topCategory} </if>"
            + "<if test='category != null'>AND a.category_id = #{category} </if>"
            + "<if test='level != null'>AND a.security_level = #{level} </if>"
            + "</script>")
    Long countSearch(@Param("kw") String kw,
                     @Param("topCategory") Long topCategory,
                     @Param("category") Long category,
                     @Param("level") Integer level,
                     @Param("maxLevel") int maxLevel);

    /**
     * 分页检索列表。orderBy 由 Service 白名单生成，形如 {@code a.heat DESC, a.id ASC}。
     */
    @Select("<script>"
            + "SELECT a.* FROM dr_archive a " + BASE_WHERE
            + "<if test='kw != null and kw != \"\"'>AND " + KW_COND + "</if>"
            + "<if test='topCategory != null'>AND a.top_category_id = #{topCategory} </if>"
            + "<if test='category != null'>AND a.category_id = #{category} </if>"
            + "<if test='level != null'>AND a.security_level = #{level} </if>"
            + "ORDER BY ${orderBy} LIMIT #{offset}, #{size}"
            + "</script>")
    List<DrArchive> listSearch(@Param("kw") String kw,
                               @Param("topCategory") Long topCategory,
                               @Param("category") Long category,
                               @Param("level") Integer level,
                               @Param("maxLevel") int maxLevel,
                               @Param("orderBy") String orderBy,
                               @Param("offset") long offset,
                               @Param("size") long size);

    // ------------------------------------------------------------------
    // 分面统计（分级 / 分类维度各自命中多少条）
    // ------------------------------------------------------------------

    /**
     * 一级分类分面。对其他维度（关键词、密级）生效，忽略「分类」自身维度，
     * 这样每个一级分类都能显示出「如果我选它会有多少条」。
     */
    @Select("<script>"
            + "SELECT c.id AS `key`, c.name AS label, COUNT(a.id) AS total "
            + "FROM dr_archive_category c "
            + "LEFT JOIN dr_archive a ON a.top_category_id = c.id "
            + "  AND a.deleted = 0 AND a.status = 1 AND a.security_level &lt;= #{maxLevel} "
            + "<if test='kw != null and kw != \"\"'>AND " + KW_COND + "</if>"
            + "<if test='level != null'>AND a.security_level = #{level} </if>"
            + "WHERE c.parent_id = 0 AND c.status = 1 "
            + "GROUP BY c.id, c.name, c.sort_no ORDER BY c.sort_no"
            + "</script>")
    List<FacetVO> facetTopCategory(@Param("kw") String kw,
                                   @Param("level") Integer level,
                                   @Param("maxLevel") int maxLevel);

    /**
     * 二级分类分面。与一级分类分面同理：只对关键词、密级生效，
     * 忽略「分类」自身维度，保证同级筛选项之间的条数口径一致。
     */
    @Select("<script>"
            + "SELECT c.id AS `key`, c.name AS label, COUNT(a.id) AS total "
            + "FROM dr_archive_category c "
            + "LEFT JOIN dr_archive a ON a.category_id = c.id "
            + "  AND a.deleted = 0 AND a.status = 1 AND a.security_level &lt;= #{maxLevel} "
            + "<if test='kw != null and kw != \"\"'>AND " + KW_COND + "</if>"
            + "<if test='level != null'>AND a.security_level = #{level} </if>"
            + "WHERE c.parent_id &lt;&gt; 0 AND c.status = 1 "
            + "GROUP BY c.id, c.name, c.sort_no ORDER BY c.sort_no"
            + "</script>")
    List<FacetVO> facetCategory(@Param("kw") String kw,
                                @Param("level") Integer level,
                                @Param("maxLevel") int maxLevel);

    /**
     * 密级分面。对关键词与两级分类生效，忽略「密级」自身维度。
     */
    @Select("<script>"
            + "SELECT a.security_level AS `key`, "
            + "  CASE a.security_level WHEN 1 THEN '公开' WHEN 2 THEN '内部' "
            + "       WHEN 3 THEN '机密' ELSE '绝密' END AS label, "
            + "  COUNT(*) AS total "
            + "FROM dr_archive a " + BASE_WHERE
            + "<if test='kw != null and kw != \"\"'>AND " + KW_COND + "</if>"
            + "<if test='topCategory != null'>AND a.top_category_id = #{topCategory} </if>"
            + "<if test='category != null'>AND a.category_id = #{category} </if>"
            + "GROUP BY a.security_level ORDER BY a.security_level"
            + "</script>")
    List<FacetVO> facetLevel(@Param("kw") String kw,
                             @Param("topCategory") Long topCategory,
                             @Param("category") Long category,
                             @Param("maxLevel") int maxLevel);

    // ------------------------------------------------------------------
    // 首页展示
    // ------------------------------------------------------------------

    /** 各一级分类下的可见档案数（首页分类树用） */
    @Select("SELECT top_category_id AS id, COUNT(*) AS total FROM dr_archive "
            + "WHERE deleted = 0 AND status = 1 AND security_level <= #{maxLevel} "
            + "GROUP BY top_category_id")
    List<Map<String, Object>> countByTopCategory(@Param("maxLevel") int maxLevel);

    /** 各二级分类下的可见档案数（首页分类树用） */
    @Select("SELECT category_id AS id, COUNT(*) AS total FROM dr_archive "
            + "WHERE deleted = 0 AND status = 1 AND security_level <= #{maxLevel} "
            + "GROUP BY category_id")
    List<Map<String, Object>> countByCategory(@Param("maxLevel") int maxLevel);

    /** 可见档案总数 */
    @Select("SELECT COUNT(*) FROM dr_archive "
            + "WHERE deleted = 0 AND status = 1 AND security_level <= #{maxLevel}")
    Long countVisible(@Param("maxLevel") int maxLevel);

    /** 可见的精选卷宗数 */
    @Select("SELECT COUNT(*) FROM dr_archive "
            + "WHERE deleted = 0 AND status = 1 AND security_level <= #{maxLevel} AND spotlight = 1")
    Long countSpotlight(@Param("maxLevel") int maxLevel);

    /** 精选卷宗 */
    @Select("SELECT * FROM dr_archive "
            + "WHERE deleted = 0 AND status = 1 AND security_level <= #{maxLevel} AND spotlight = 1 "
            + "ORDER BY heat DESC, id ASC LIMIT #{limit}")
    List<DrArchive> listSpotlight(@Param("maxLevel") int maxLevel, @Param("limit") long limit);

    /** 热门卷宗 */
    @Select("SELECT * FROM dr_archive "
            + "WHERE deleted = 0 AND status = 1 AND security_level <= #{maxLevel} "
            + "ORDER BY heat DESC, id ASC LIMIT #{limit}")
    List<DrArchive> listHot(@Param("maxLevel") int maxLevel, @Param("limit") long limit);

    /** 最新归档 */
    @Select("SELECT * FROM dr_archive "
            + "WHERE deleted = 0 AND status = 1 AND security_level <= #{maxLevel} "
            + "ORDER BY create_time DESC, id DESC LIMIT #{limit}")
    List<DrArchive> listLatest(@Param("maxLevel") int maxLevel, @Param("limit") long limit);

    /** 详情页按 id 查（带上架状态校验） */
    @Select("SELECT * FROM dr_archive WHERE id = #{id} AND deleted = 0 LIMIT 1")
    DrArchive selectVisibleById(@Param("id") Long id);

    /** 浏览量 +1 */
    @Update("UPDATE dr_archive SET view_count = view_count + 1 WHERE id = #{id}")
    void incrViewCount(@Param("id") Long id);
}
