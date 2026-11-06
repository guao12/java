package com.fenglin.springboottest.service.impl;

import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.common.CurrentAdmin;
import com.fenglin.springboottest.common.CurrentUser;
import com.fenglin.springboottest.dto.ReportCreateDTO;
import com.fenglin.springboottest.entity.DrAdmin;
import com.fenglin.springboottest.entity.DrReply;
import com.fenglin.springboottest.entity.DrReport;
import com.fenglin.springboottest.entity.DrThread;
import com.fenglin.springboottest.entity.DrUser;
import com.fenglin.springboottest.mapper.DrReplyMapper;
import com.fenglin.springboottest.mapper.DrReportMapper;
import com.fenglin.springboottest.mapper.DrThreadMapper;
import com.fenglin.springboottest.service.ReportService;
import com.fenglin.springboottest.vo.PageVO;
import com.fenglin.springboottest.vo.ReportVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 举报服务实现。
 *
 * @author fenglin
 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final long DEFAULT_SIZE = 20L;
    private static final long MAX_SIZE = 100L;

    /** 举报原因 → 中文（后台与前端共用同一套 key，避免两边各写一份文案） */
    private static final Map<String, String> REASON_LABELS = Map.of(
            "spam", "广告 / 引流",
            "spoiler", "剧透未标注",
            "copyright", "侵犯版权 / 未授权转载",
            "abuse", "人身攻击 / 引战",
            "other", "其他"
    );

    private final DrReportMapper reportMapper;
    private final DrThreadMapper threadMapper;
    private final DrReplyMapper replyMapper;

    public ReportServiceImpl(DrReportMapper reportMapper,
                             DrThreadMapper threadMapper,
                             DrReplyMapper replyMapper) {
        this.reportMapper = reportMapper;
        this.threadMapper = threadMapper;
        this.replyMapper = replyMapper;
    }

    // ==================================================================
    // 读者侧：提交举报
    // ==================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long report(ReportCreateDTO dto) {
        DrUser user = requireLogin();
        String type = normalizeType(dto.getTargetType());
        String reason = normalizeReason(dto.getReason());
        if (dto.getTargetId() == null) {
            throw new BizException("举报对象不能为空");
        }

        // 顺着被举报对象反查主题与作者：不信任前端传来的 threadId，
        // 否则有人可以伪造 threadId 把举报挂到别的帖子下，干扰后台判断
        Long threadId;
        Long authorId;
        if (DrReport.TARGET_THREAD.equals(type)) {
            DrThread t = threadMapper.selectLive(dto.getTargetId());
            if (t == null) {
                throw new BizException("该主题不存在或已下线");
            }
            threadId = t.getId();
            authorId = t.getAuthorId();
        } else {
            DrReply r = replyMapper.selectById(dto.getTargetId());
            if (r == null || r.getDeleted() != null && r.getDeleted() == 1) {
                throw new BizException("该回复不存在或已删除");
            }
            threadId = r.getThreadId();
            authorId = r.getAuthorId();
        }
        if (authorId != null && authorId.equals(user.getId())) {
            throw new BizException("不用举报自己的内容，直接编辑或删除即可");
        }

        Long dup = reportMapper.countByReporter(type, dto.getTargetId(), user.getId());
        if (dup != null && dup > 0) {
            throw new BizException("你已经举报过这条内容，我们处理后会同步结果");
        }

        DrReport row = new DrReport();
        row.setTargetType(type);
        row.setTargetId(dto.getTargetId());
        row.setThreadId(threadId);
        row.setReporterId(user.getId());
        row.setReporterName(safe(user.getCodename()));
        row.setReason(reason);
        row.setDetail(trimToNull(dto.getDetail()));
        row.setStatus(DrReport.STATUS_PENDING);
        row.setCreateTime(LocalDateTime.now());
        reportMapper.insert(row);
        return row.getId();
    }

    // ==================================================================
    // 后台侧：队列与处理
    // ==================================================================

    @Override
    public PageVO<ReportVO> list(int status, long page, long size) {
        long p = Math.max(1L, page);
        long s = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        boolean all = status < 0;

        Long counted = all ? reportMapper.countAll() : reportMapper.countByStatus(status);
        long total = counted == null ? 0L : counted;
        List<DrReport> rows;
        if (total == 0) {
            rows = Collections.emptyList();
        } else if (all) {
            rows = reportMapper.selectPageList((p - 1) * s, s);
        } else {
            rows = reportMapper.listByStatus(status, (p - 1) * s, s);
        }
        return new PageVO<>(toVOs(rows), total, p, s);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, String action, String note) {
        DrAdmin admin = CurrentAdmin.get();
        if (admin == null) {
            throw new BizException(401, "请先登录后台");
        }
        DrReport row = reportMapper.selectById(id);
        if (row == null) {
            throw new BizException("举报记录不存在");
        }
        if (row.getStatus() != null && row.getStatus() != DrReport.STATUS_PENDING) {
            throw new BizException("该举报已处理过");
        }

        boolean takedown = "takedown".equalsIgnoreCase(action);
        if (!takedown && !"reject".equalsIgnoreCase(action)) {
            throw new BizException("未知的处理动作");
        }

        if (takedown) {
            if (DrReport.TARGET_THREAD.equals(row.getTargetType())) {
                takeDownThread(row.getTargetId());
            } else {
                takeDownReply(row.getTargetId());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        row.setStatus(takedown ? DrReport.STATUS_TAKEN_DOWN : DrReport.STATUS_REJECTED);
        row.setHandlerId(admin.getId());
        row.setHandleNote(trimToNull(note));
        row.setHandleTime(now);
        reportMapper.updateById(row);

        // 同一个对象上的其他待处理工单一起结案：
        // 内容都已经下架 / 已判定无问题，留着别人再处理一遍纯属浪费
        for (DrReport other : reportMapper.listPendingByTarget(row.getTargetType(), row.getTargetId())) {
            if (other.getId().equals(row.getId())) {
                continue;
            }
            other.setStatus(row.getStatus());
            other.setHandlerId(admin.getId());
            other.setHandleNote("随同一对象的处理结果一并结案");
            other.setHandleTime(now);
            reportMapper.updateById(other);
        }
    }

    @Override
    public Long pendingCount() {
        return reportMapper.countByStatus(DrReport.STATUS_PENDING);
    }

    // ==================================================================
    // 下架动作
    // ==================================================================

    /** 主题下架：status=0，之后 selectLive 查不到，等同从论坛消失 */
    private void takeDownThread(Long threadId) {
        DrThread t = threadMapper.selectById(threadId);
        if (t == null) {
            return;
        }
        t.setStatus(0);
        t.setUpdateTime(LocalDateTime.now());
        threadMapper.updateById(t);
    }

    /** 楼层下架：status=0，并把主题的楼层数与热度重算一遍，避免列表数字对不上 */
    private void takeDownReply(Long replyId) {
        DrReply r = replyMapper.selectById(replyId);
        if (r == null) {
            return;
        }
        r.setStatus(0);
        r.setUpdateTime(LocalDateTime.now());
        replyMapper.updateById(r);

        Long counted = replyMapper.countByThread(r.getThreadId());
        int replyCount = counted == null ? 0 : counted.intValue();
        DrThread t = threadMapper.selectById(r.getThreadId());
        threadMapper.updateAfterReply(r.getThreadId(), replyCount,
                t == null || t.getLastReplyAt() == null ? LocalDateTime.now() : t.getLastReplyAt());
    }

    // ==================================================================
    // 组装
    // ==================================================================

    private List<ReportVO> toVOs(List<DrReport> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        List<ReportVO> list = new ArrayList<>(rows.size());
        for (DrReport r : rows) {
            ReportVO vo = new ReportVO();
            vo.setId(r.getId());
            vo.setTargetType(r.getTargetType());
            vo.setTargetId(r.getTargetId());
            vo.setThreadId(r.getThreadId());
            vo.setReporterId(r.getReporterId());
            vo.setReporterName(safe(r.getReporterName()));
            vo.setReason(r.getReason());
            vo.setReasonLabel(REASON_LABELS.getOrDefault(r.getReason(), "其他"));
            vo.setDetail(safe(r.getDetail()));
            vo.setStatus(r.getStatus());
            vo.setStatusLabel(statusLabel(r.getStatus()));
            vo.setCreateTime(r.getCreateTime() == null ? "" : ISO.format(r.getCreateTime()));
            vo.setHandleTime(r.getHandleTime() == null ? "" : ISO.format(r.getHandleTime()));
            vo.setHandleNote(safe(r.getHandleNote()));

            // 内容快照：被举报内容可能已经下架，快照让处理记录仍能说明「当时举报的是什么」
            DrThread t = threadMapper.selectById(r.getThreadId());
            vo.setThreadTitle(t == null ? "（主题已删除）" : safe(t.getTitle()));
            if (DrReport.TARGET_REPLY.equals(r.getTargetType())) {
                DrReply reply = replyMapper.selectById(r.getTargetId());
                if (reply != null) {
                    vo.setFloorNo(reply.getFloorNo());
                    vo.setTargetExcerpt(excerpt(reply.getContent(), 80));
                    vo.setTargetAuthor(safe(reply.getAuthorName()));
                }
            } else if (t != null) {
                vo.setTargetExcerpt(excerpt(t.getContent(), 80));
                vo.setTargetAuthor(safe(t.getAuthorName()));
            }
            list.add(vo);
        }
        return list;
    }

    private static String statusLabel(Integer status) {
        if (status == null) {
            return "待处理";
        }
        switch (status) {
            case DrReport.STATUS_TAKEN_DOWN:
                return "已下架";
            case DrReport.STATUS_REJECTED:
                return "已驳回";
            default:
                return "待处理";
        }
    }

    private static String normalizeType(String type) {
        String t = type == null ? "" : type.trim().toLowerCase();
        if (!DrReport.TARGET_THREAD.equals(t) && !DrReport.TARGET_REPLY.equals(t)) {
            throw new BizException("举报对象类型不合法");
        }
        return t;
    }

    private static String normalizeReason(String reason) {
        String r = reason == null ? "" : reason.trim().toLowerCase();
        if (!REASON_LABELS.containsKey(r)) {
            throw new BizException("举报原因不合法");
        }
        return r;
    }

    private static DrUser requireLogin() {
        DrUser user = CurrentUser.get();
        if (user == null) {
            throw new BizException(401, "请先登录后再举报");
        }
        return user;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String excerpt(String s, int max) {
        if (s == null) {
            return "";
        }
        String flat = s.replaceAll("\\s+", " ").trim();
        return flat.length() <= max ? flat : flat.substring(0, max) + "…";
    }
}
