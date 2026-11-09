package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.common.CurrentAdmin;
import com.fenglin.springboottest.common.SecurityLevel;
import com.fenglin.springboottest.dto.ThreadAdminSaveDTO;
import com.fenglin.springboottest.entity.DrAdmin;
import com.fenglin.springboottest.entity.DrBoard;
import com.fenglin.springboottest.entity.DrThread;
import com.fenglin.springboottest.mapper.DrBoardMapper;
import com.fenglin.springboottest.mapper.DrThreadMapper;
import com.fenglin.springboottest.service.AdminThreadService;
import com.fenglin.springboottest.vo.AdminThreadVO;
import com.fenglin.springboottest.vo.PageVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台「主题管理」服务实现。
 *
 * @author fenglin
 */
@Service
public class AdminThreadServiceImpl implements AdminThreadService {

    private static final Logger log = LoggerFactory.getLogger(AdminThreadServiceImpl.class);

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final long DEFAULT_SIZE = 20L;
    private static final long MAX_SIZE = 50L;

    /** 官方帖的 author_id 固定为 0：dr_thread.author_id 指向 dr_user，
     *  管理员不是学员，塞一个假的 user id 会让「我的帖子」等查询出现幽灵数据。 */
    private static final long OFFICIAL_AUTHOR_ID = 0L;

    /** 署名身份按「教员」算：前台据此显示身份标签，也与馆藏旧帖的展示口径一致 */
    private static final String OFFICIAL_IDENTITY = "teacher";

    private static final int EXCERPT_LEN = 80;

    private final DrThreadMapper threadMapper;
    private final DrBoardMapper boardMapper;

    public AdminThreadServiceImpl(DrThreadMapper threadMapper, DrBoardMapper boardMapper) {
        this.threadMapper = threadMapper;
        this.boardMapper = boardMapper;
    }

    @Override
    public List<DrBoard> boards() {
        return boardMapper.selectList(
                new LambdaQueryWrapper<DrBoard>().orderByAsc(DrBoard::getSortNo).orderByAsc(DrBoard::getId));
    }

    @Override
    public PageVO<AdminThreadVO> list(Long boardId, Integer status, String keyword, long page, long size) {
        checkStatus(status);

        long p = Math.max(1L, page);
        long s = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String kw = trimToNull(keyword);

        Long total = threadMapper.adminCount(boardId, status, kw);
        if (total == null || total == 0) {
            return new PageVO<>(Collections.emptyList(), 0L, p, s);
        }

        List<DrThread> rows = threadMapper.adminList(boardId, status, kw, (p - 1) * s, s);
        Map<Long, DrBoard> index = boardIndex(rows);
        List<AdminThreadVO> list = rows.stream()
                .map(row -> toVO(row, index.get(row.getBoardId())))
                .collect(Collectors.toList());
        return new PageVO<>(list, total, p, s);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminThreadVO create(ThreadAdminSaveDTO dto) {
        DrBoard board = requireBoard(dto.getBoardId());
        String title = requireText(dto.getTitle(), 4, 80, "标题需 4–80 字");
        String content = requireText(dto.getContent(), 10, 8000, "正文需 10–8000 字");
        int level = SecurityLevel.normalize(dto.getSecurityLevel());
        int status = dto.getStatus() == null ? 1 : dto.getStatus();
        checkStatus(status);

        DrThread row = new DrThread();
        row.setBoardId(board.getId());
        row.setAuthorId(OFFICIAL_AUTHOR_ID);
        row.setAuthorName(adminName());
        row.setAuthorIdentity(OFFICIAL_IDENTITY);
        row.setTitle(title);
        row.setContent(content);
        row.setSecurityLevel(level);
        row.setReplyVisible(Boolean.TRUE.equals(dto.getReplyVisible()) ? 1 : 0);
        row.setPinned(Boolean.TRUE.equals(dto.getPinned()) ? 1 : 0);
        row.setEssence(Boolean.TRUE.equals(dto.getEssence()) ? 1 : 0);
        row.setViewCount(0);
        row.setReplyCount(0);
        row.setLikeCount(0);
        row.setHeat(0);
        row.setStatus(status);
        row.setDeleted(0);
        row.setLastReplyAt(LocalDateTime.now());
        threadMapper.insert(row);

        DrThread saved = threadMapper.selectById(row.getId());
        if (saved == null) {
            throw new BizException("主题创建失败，请重试");
        }
        log.info("后台新建主题：id={} board={} title={} by={}",
                saved.getId(), board.getCode(), title, adminName());
        return toVO(saved, board);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminThreadVO update(Long id, ThreadAdminSaveDTO dto) {
        DrThread row = threadMapper.selectById(id);
        if (row == null) {
            throw new BizException("该主题不存在或已被删除");
        }
        DrBoard board = requireBoard(dto.getBoardId());
        String title = requireText(dto.getTitle(), 4, 80, "标题需 4–80 字");
        String content = requireText(dto.getContent(), 10, 8000, "正文需 10–8000 字");
        int level = SecurityLevel.normalize(dto.getSecurityLevel());
        int status = dto.getStatus() == null ? (row.getStatus() == null ? 1 : row.getStatus()) : dto.getStatus();
        checkStatus(status);

        // 只改内容与版务字段，author_id / author_name / 各项计数一律不动：
        // 管理员改的是「这篇帖子长什么样」，不是「它是谁写的」。
        row.setBoardId(board.getId());
        row.setTitle(title);
        row.setContent(content);
        row.setSecurityLevel(level);
        row.setReplyVisible(Boolean.TRUE.equals(dto.getReplyVisible()) ? 1 : 0);
        row.setPinned(Boolean.TRUE.equals(dto.getPinned()) ? 1 : 0);
        row.setEssence(Boolean.TRUE.equals(dto.getEssence()) ? 1 : 0);
        row.setStatus(status);
        row.setUpdateTime(LocalDateTime.now());
        threadMapper.updateById(row);

        DrThread saved = threadMapper.selectById(id);
        log.info("后台编辑主题：id={} title={} status={} by={}", id, title, status, adminName());
        return toVO(saved == null ? row : saved, board);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DrThread row = threadMapper.selectById(id);
        if (row == null) {
            throw new BizException("该主题不存在或已被删除");
        }
        // deleted 带 @TableLogic，updateById 会静默忽略这个字段，逻辑删除必须走 deleteById
        threadMapper.deleteById(id);
        log.info("后台删除主题：id={} title={} by={}", id, row.getTitle(), adminName());
    }

    /* ================= 内部方法 ================= */

    /** 当前管理员署名（昵称为空时退回账号名） */
    private String adminName() {
        DrAdmin admin = CurrentAdmin.get();
        if (admin == null) {
            return "学院管理处";
        }
        String nickname = trimToNull(admin.getNickname());
        return nickname != null ? nickname : trimToNull(admin.getUsername());
    }

    private DrBoard requireBoard(Long boardId) {
        if (boardId == null) {
            throw new BizException("请选择版块");
        }
        DrBoard board = boardMapper.selectById(boardId);
        if (board == null) {
            throw new BizException("版块不存在");
        }
        if (board.getStatus() == null || board.getStatus() != 1) {
            throw new BizException("版块「" + safe(board.getName()) + "」已关闭，不能发布主题");
        }
        return board;
    }

    private void checkStatus(Integer status) {
        if (status != null && status != 1 && status != 0 && status != -1) {
            throw new BizException("主题状态只能是：1 正常 / 0 待审 / -1 已下线");
        }
    }

    private Map<Long, DrBoard> boardIndex(List<DrThread> rows) {
        List<Long> ids = rows.stream().map(DrThread::getBoardId).filter(java.util.Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, DrBoard> index = new HashMap<>();
        if (ids.isEmpty()) {
            return index;
        }
        boardMapper.selectBatchIds(ids).forEach(b -> index.put(b.getId(), b));
        return index;
    }

    private AdminThreadVO toVO(DrThread row, DrBoard board) {
        AdminThreadVO vo = new AdminThreadVO();
        vo.setId(row.getId());
        vo.setBoardId(row.getBoardId());
        if (board != null) {
            vo.setBoardCode(board.getCode());
            vo.setBoardName(board.getName());
        } else {
            vo.setBoardName("（版块已删除）");
        }
        vo.setTitle(row.getTitle());
        vo.setContent(row.getContent());
        vo.setExcerpt(excerpt(row.getContent()));
        vo.setAuthorId(row.getAuthorId());
        vo.setAuthorName(safe(row.getAuthorName()));
        int level = SecurityLevel.normalize(row.getSecurityLevel());
        vo.setSecurityLevel(level);
        vo.setSecurityLabel(SecurityLevel.label(level));
        vo.setReplyVisible(row.getReplyVisible() != null && row.getReplyVisible() == 1);
        vo.setPinned(row.getPinned() != null && row.getPinned() == 1);
        vo.setEssence(row.getEssence() != null && row.getEssence() == 1);
        vo.setViewCount(nz(row.getViewCount()));
        vo.setReplyCount(nz(row.getReplyCount()));
        vo.setLikeCount(nz(row.getLikeCount()));
        vo.setHeat(nz(row.getHeat()));
        vo.setStatus(row.getStatus() == null ? 1 : row.getStatus());
        vo.setStatusLabel(statusLabel(vo.getStatus()));
        vo.setCreatedAt(iso(row.getCreateTime()));
        vo.setUpdatedAt(iso(row.getUpdateTime()));
        return vo;
    }

    private static String statusLabel(int status) {
        if (status == 0) {
            return "待审";
        }
        if (status == -1) {
            return "已下线";
        }
        return "正常";
    }

    /** 摘要：换行压成空格再截断，避免列表里出现断行把行高撑乱 */
    private static String excerpt(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String flat = content.replaceAll("\\s+", " ").trim();
        return flat.length() <= EXCERPT_LEN ? flat : flat.substring(0, EXCERPT_LEN) + "…";
    }

    private static String requireText(String text, int min, int max, String message) {
        String t = text == null ? "" : text.trim();
        if (t.length() < min || t.length() > max) {
            throw new BizException(message);
        }
        return t;
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

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static String iso(LocalDateTime time) {
        return time == null ? null : time.format(ISO);
    }
}
