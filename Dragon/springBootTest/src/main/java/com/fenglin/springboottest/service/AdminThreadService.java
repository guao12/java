package com.fenglin.springboottest.service;

import com.fenglin.springboottest.dto.ThreadAdminSaveDTO;
import com.fenglin.springboottest.entity.DrBoard;
import com.fenglin.springboottest.vo.AdminThreadVO;
import com.fenglin.springboottest.vo.PageVO;

import java.util.List;

/**
 * 后台「主题管理」服务：论坛帖子的新建、编辑、置顶加精、上下线、删除。
 *
 * <p>与 {@link ForumService} 的分工：</p>
 * <ul>
 *   <li>{@code ForumService} 是读者视角 —— 有密级、权限点、发帖频率、敏感词那一整套门禁；</li>
 *   <li>本服务是管理员视角 —— 不做密级过滤，能改任何人的帖子，
 *       但也不替读者判断可见性，前台该怎么过滤还怎么过滤。</li>
 * </ul>
 *
 * <p>唯一的交集是数据本身：两边改的都是 dr_thread，所以冗余计数（reply_count 等）
 * 只在读者侧的回帖 / 点赞流程里维护，这里不重复计算。</p>
 *
 * @author fenglin
 */
public interface AdminThreadService {

    /** 版块下拉（含已关闭版块，管理员要能看到并说明为什么发不进去） */
    List<DrBoard> boards();

    /**
     * 后台主题分页。
     *
     * @param boardId 版块主键，null = 全部版块
     * @param status  1 正常 / 0 待审 / -1 已下线，null = 全部
     * @param keyword 标题 / 正文 / 作者关键词，可空
     * @param page    从 1 开始
     * @param size    1–50，超出按 50
     */
    PageVO<AdminThreadVO> list(Long boardId, Integer status, String keyword, long page, long size);

    /** 新建主题（署名记为当前管理员，author_id 固定 0 表示「官方发布」） */
    AdminThreadVO create(ThreadAdminSaveDTO dto);

    /** 编辑主题（不改作者署名，改了等于篡改历史） */
    AdminThreadVO update(Long id, ThreadAdminSaveDTO dto);

    /** 删除主题（逻辑删除，楼层一并不可见） */
    void delete(Long id);
}
