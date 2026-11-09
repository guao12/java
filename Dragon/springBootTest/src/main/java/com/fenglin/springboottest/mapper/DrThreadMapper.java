package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DrThread;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 论坛主题 Mapper。
 *
 * <p>与 {@code DrArchiveMapper} 保持同一套写法：项目未引入分页插件，
 * LIMIT / COUNT 全部手写；排序字段由 Service 白名单生成，绝不接受前端传列名。</p>
 *
 * <p>所有查询显式带 {@code deleted = 0 AND status = 1}；面向访客的入口
 * （最新 / 最热 / 检索）额外带 {@code security_level <= maxLevel}。</p>
 *
 * @author fenglin
 */
@Mapper
public interface DrThreadMapper extends BaseMapper<DrThread> {

    /** 基础过滤：未删除 + 正常状态 */
    String BASE_WHERE = "WHERE t.deleted = 0 AND t.status = 1 ";

    /** 关键词命中：标题 / 正文 / 作者代号 */
    String KW_COND = "(t.title LIKE CONCAT('%', #{keyword}, '%') "
            + "OR t.content LIKE CONCAT('%', #{keyword}, '%') "
            + "OR t.author_name LIKE CONCAT('%', #{keyword}, '%')) ";

    // ------------------------------------------------------------------
    // 版块内列表
    // ------------------------------------------------------------------

    /** 版块内主题总数 */
    @Select("<script>"
            + "SELECT COUNT(*) FROM dr_thread t "
            + "WHERE t.deleted = 0 AND t.status = 1 AND t.board_id = #{boardId} "
            + "<if test='essenceOnly'>AND t.essence = 1 </if>"
            + "<if test='keyword != null and keyword != \"\"'>AND " + KW_COND + "</if>"
            + "</script>")
    Long countByBoard(@Param("boardId") Long boardId,
                      @Param("keyword") String keyword,
                      @Param("essenceOnly") boolean essenceOnly);

    /** 版块内分页列表。orderBy 由 Service 白名单生成 */
    @Select("<script>"
            + "SELECT t.* FROM dr_thread t "
            + "WHERE t.deleted = 0 AND t.status = 1 AND t.board_id = #{boardId} "
            + "<if test='essenceOnly'>AND t.essence = 1 </if>"
            + "<if test='keyword != null and keyword != \"\"'>AND " + KW_COND + "</if>"
            + "ORDER BY ${orderBy} LIMIT #{offset}, #{size}"
            + "</script>")
    List<DrThread> listByBoard(@Param("boardId") Long boardId,
                               @Param("keyword") String keyword,
                               @Param("essenceOnly") boolean essenceOnly,
                               @Param("orderBy") String orderBy,
                               @Param("offset") long offset,
                               @Param("size") long size);

    // ------------------------------------------------------------------
    // 首页 / 检索
    // ------------------------------------------------------------------

    /** 最新主题（按密级过滤） */
    @Select("SELECT * FROM dr_thread "
            + "WHERE deleted = 0 AND status = 1 AND security_level <= #{maxLevel} "
            + "ORDER BY COALESCE(last_reply_at, create_time) DESC, id DESC LIMIT #{limit}")
    List<DrThread> listLatest(@Param("maxLevel") int maxLevel, @Param("limit") long limit);

    /** 热门主题（按密级过滤，全时段累计 heat，作为兜底） */
    @Select("SELECT * FROM dr_thread "
            + "WHERE deleted = 0 AND status = 1 AND security_level <= #{maxLevel} "
            + "ORDER BY heat DESC, id DESC LIMIT #{limit}")
    List<DrThread> listHot(@Param("maxLevel") int maxLevel, @Param("limit") long limit);

    /**
     * 近期活跃的热门主题（按密级过滤）。
     *
     * <p>heat 是累计值：只按 heat 排序等于「历史总榜」，半年前的神帖会永远霸占首页，
     * 新内容起不来。加时间窗之后 heat 才真正等于「最近大家在聊什么」。
     * 时间锚点用 last_reply_at；从没人回复过的新帖退回 create_time。</p>
     */
    @Select("SELECT * FROM dr_thread "
            + "WHERE deleted = 0 AND status = 1 AND security_level <= #{maxLevel} "
            + "AND (last_reply_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) "
            + "     OR (last_reply_at IS NULL AND create_time >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR))) "
            + "ORDER BY heat DESC, id DESC LIMIT #{limit}")
    List<DrThread> listHotRecent(@Param("maxLevel") int maxLevel,
                                 @Param("hours") int hours,
                                 @Param("limit") long limit);

    /**
     * 我收藏的主题，按收藏时间倒序。
     *
     * <p>排序用 collect 动作的时间而不是发帖时间：收藏夹是「我什么时候存的」，
     * 按发帖时间排会让人找不到刚存的那篇。</p>
     */
    @Select("SELECT t.* FROM dr_thread t "
            + "JOIN dr_thread_action a ON a.thread_id = t.id AND a.type = 'collect' AND a.user_id = #{userId} "
            + "WHERE t.deleted = 0 AND t.status = 1 AND t.security_level <= #{maxLevel} "
            + "ORDER BY a.create_time DESC, t.id DESC LIMIT #{size} OFFSET #{offset}")
    List<DrThread> listCollected(@Param("userId") Long userId,
                                 @Param("maxLevel") int maxLevel,
                                 @Param("offset") long offset,
                                 @Param("size") long size);

    /** 我收藏的主题总数（按同一套可见性过滤） */
    @Select("SELECT COUNT(*) FROM dr_thread t "
            + "JOIN dr_thread_action a ON a.thread_id = t.id AND a.type = 'collect' AND a.user_id = #{userId} "
            + "WHERE t.deleted = 0 AND t.status = 1 AND t.security_level <= #{maxLevel}")
    Long countCollected(@Param("userId") Long userId, @Param("maxLevel") int maxLevel);

    /** 全站检索（标题 / 正文 / 作者），按密级过滤 */
    @Select("<script>"
            + "SELECT * FROM dr_thread t "
            + "WHERE t.deleted = 0 AND t.status = 1 AND t.security_level &lt;= #{maxLevel} "
            + "AND " + KW_COND
            + "ORDER BY t.heat DESC, t.id DESC LIMIT #{limit}"
            + "</script>")
    List<DrThread> search(@Param("keyword") String keyword,
                          @Param("maxLevel") int maxLevel,
                          @Param("limit") long limit);

    /** 某作者的主题（我的档案页） */
    @Select("SELECT * FROM dr_thread "
            + "WHERE deleted = 0 AND status = 1 AND author_id = #{authorId} "
            + "ORDER BY create_time DESC, id DESC LIMIT #{limit}")
    List<DrThread> listByAuthor(@Param("authorId") Long authorId, @Param("limit") long limit);

    /** 某作者的批注数 */
    @Select("SELECT COUNT(*) FROM dr_thread WHERE deleted = 0 AND status = 1 AND author_id = #{authorId}")
    Long countByAuthor(@Param("authorId") Long authorId);

    // ------------------------------------------------------------------
    // 统计
    // ------------------------------------------------------------------

    /** 可见主题总数 */
    @Select("SELECT COUNT(*) FROM dr_thread "
            + "WHERE deleted = 0 AND status = 1 AND security_level <= #{maxLevel}")
    Long countVisible(@Param("maxLevel") int maxLevel);

    /** 可见主题的总浏览量（首页统计用） */
    @Select("SELECT COALESCE(SUM(view_count), 0) FROM dr_thread "
            + "WHERE deleted = 0 AND status = 1 AND security_level <= #{maxLevel}")
    Long sumVisibleViews(@Param("maxLevel") int maxLevel);

    /** 今日新主题数（按密级过滤） */
    @Select("SELECT COUNT(*) FROM dr_thread "
            + "WHERE deleted = 0 AND status = 1 AND security_level <= #{maxLevel} "
            + "AND create_time >= CURDATE()")
    Long countToday(@Param("maxLevel") int maxLevel);

    /** 各版块主题数，供首页版块卡片显示 */
    @Select("SELECT board_id AS boardId, COUNT(*) AS total FROM dr_thread "
            + "WHERE deleted = 0 AND status = 1 AND security_level <= #{maxLevel} "
            + "GROUP BY board_id")
    List<Map<String, Object>> countGroupByBoard(@Param("maxLevel") int maxLevel);

    /** 各版块最后一帖时间，供卡片显示「最后更新」 */
    @Select("SELECT board_id AS boardId, MAX(COALESCE(last_reply_at, create_time)) AS lastAt "
            + "FROM dr_thread WHERE deleted = 0 AND status = 1 AND security_level <= #{maxLevel} "
            + "GROUP BY board_id")
    List<Map<String, Object>> lastAtGroupByBoard(@Param("maxLevel") int maxLevel);

    // ------------------------------------------------------------------
    // 详情 / 写操作
    // ------------------------------------------------------------------

    /** 按 id 查（含状态校验，密级由 Service 判定，以便给出「封存」而非 404） */
    @Select("SELECT * FROM dr_thread WHERE id = #{id} AND deleted = 0 AND status = 1 LIMIT 1")
    DrThread selectLive(@Param("id") Long id);

    /** 浏览量 +1，同时重算热度 */
    @Update("UPDATE dr_thread SET view_count = view_count + 1, "
            + "heat = (view_count + 1) + reply_count * 3 + like_count * 5 WHERE id = #{id}")
    void incrView(@Param("id") Long id);

    /** 批注写入后同步冗余计数与最后回复时间 */
    @Update("UPDATE dr_thread SET reply_count = #{replyCount}, last_reply_at = #{lastReplyAt}, "
            + "heat = view_count + #{replyCount} * 3 + like_count * 5 WHERE id = #{id}")
    void updateAfterReply(@Param("id") Long id,
                          @Param("replyCount") int replyCount,
                          @Param("lastReplyAt") LocalDateTime lastReplyAt);

    /** 点赞数变化后同步计数与热度 */
    @Update("UPDATE dr_thread SET like_count = #{likeCount}, "
            + "heat = view_count + reply_count * 3 + #{likeCount} * 5 WHERE id = #{id}")
    void updateLikeCount(@Param("id") Long id, @Param("likeCount") int likeCount);

    /** 同一作者最近一次发帖时间，用于发帖频率限制 */
    @Select("SELECT MAX(create_time) FROM dr_thread WHERE author_id = #{authorId} AND deleted = 0")
    LocalDateTime lastPostAt(@Param("authorId") Long authorId);

    // ------------------------------------------------------------------
    // 后台主题管理（管理员视角，不做密级过滤）
    // ------------------------------------------------------------------

    /**
     * 后台主题分页。
     *
     * <p>与前台查询的三点不同：</p>
     * <ul>
     *   <li>不加 {@code security_level} 过滤 —— 管理员要能看到所有密级的帖子；</li>
     *   <li>{@code status} 可为 null（全部），也可指定 1 正常 / 0 待审 / -1 已下线；</li>
     *   <li>排序固定「置顶优先 + 最近活跃」，后台不需要用户自选排序。</li>
     * </ul>
     */
    @Select("<script>"
            + "SELECT t.* FROM dr_thread t WHERE t.deleted = 0 "
            + "<if test='boardId != null'>AND t.board_id = #{boardId} </if>"
            + "<if test='status != null'>AND t.status = #{status} </if>"
            + "<if test='keyword != null and keyword != \"\"'>AND " + KW_COND + "</if>"
            + "ORDER BY t.pinned DESC, COALESCE(t.last_reply_at, t.create_time) DESC, t.id DESC "
            + "LIMIT #{offset}, #{size}"
            + "</script>")
    List<DrThread> adminList(@Param("boardId") Long boardId,
                             @Param("status") Integer status,
                             @Param("keyword") String keyword,
                             @Param("offset") long offset,
                             @Param("size") long size);

    /** 后台主题总数，条件与 {@link #adminList} 保持一致 */
    @Select("<script>"
            + "SELECT COUNT(*) FROM dr_thread t WHERE t.deleted = 0 "
            + "<if test='boardId != null'>AND t.board_id = #{boardId} </if>"
            + "<if test='status != null'>AND t.status = #{status} </if>"
            + "<if test='keyword != null and keyword != \"\"'>AND " + KW_COND + "</if>"
            + "</script>")
    Long adminCount(@Param("boardId") Long boardId,
                    @Param("status") Integer status,
                    @Param("keyword") String keyword);
}
