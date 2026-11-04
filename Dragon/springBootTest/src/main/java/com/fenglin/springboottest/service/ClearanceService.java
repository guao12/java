package com.fenglin.springboottest.service;

import com.fenglin.springboottest.vo.ClearanceVO;

/**
 * 权限进度服务：把「权限点 / 已完成任务」持久化到数据库，实现跨设备同步。
 *
 * @author fenglin
 */
public interface ClearanceService {

    /**
     * 读取某用户的权限进度。从未同步过时返回空记录（points=0）。
     *
     * @param userId dr_user.id
     */
    ClearanceVO get(Long userId);

    /**
     * 合并并保存权限进度。
     *
     * <p>合并策略（宁可多不可少，避免多设备互相覆盖丢进度）：</p>
     * <ul>
     *   <li>points 取两者较大值；</li>
     *   <li>completed 取并集（保序去重）；</li>
     *   <li>records 按任务编号去重，保留 at 更新的一条，整体按时间倒序。</li>
     * </ul>
     *
     * @param userId   当前登录用户 id
     * @param account  当前登录学号（冗余写入，便于排查）
     * @param incoming 前端上报的进度
     * @return 合并后的最终进度（前端应以这份为准回写本地）
     */
    ClearanceVO mergeAndSave(Long userId, String account, ClearanceVO incoming);
}
