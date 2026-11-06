package com.fenglin.springboottest.service;

import com.fenglin.springboottest.dto.ReportCreateDTO;
import com.fenglin.springboottest.vo.PageVO;
import com.fenglin.springboottest.vo.ReportVO;

/**
 * 内容举报服务。
 *
 * <p>两条链路：</p>
 * <ul>
 *   <li>读者侧：提交举报（必须登录，同一对象只能举报一次）；</li>
 *   <li>后台侧：待处理队列、下架 / 驳回。</li>
 * </ul>
 *
 * @author fenglin
 */
public interface ReportService {

    /**
     * 提交举报。
     *
     * @return 新建工单 id
     */
    Long report(ReportCreateDTO dto);

    /**
     * 后台队列分页。
     *
     * @param status 0 待处理 / 1 已下架 / 2 已驳回；传 -1 表示全部
     */
    PageVO<ReportVO> list(int status, long page, long size);

    /**
     * 处理工单。
     *
     * @param action takedown（举报成立，内容下架）/ reject（举报不成立）
     * @param note   处理备注，可空
     */
    void handle(Long id, String action, String note);

    /** 待处理数量（后台角标） */
    Long pendingCount();
}
