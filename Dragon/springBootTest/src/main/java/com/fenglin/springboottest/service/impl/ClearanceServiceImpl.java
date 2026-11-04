package com.fenglin.springboottest.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.DrUserClearance;
import com.fenglin.springboottest.mapper.DrUserClearanceMapper;
import com.fenglin.springboottest.service.ClearanceService;
import com.fenglin.springboottest.vo.ClearanceVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限进度服务实现。
 *
 * @author fenglin
 */
@Service
public class ClearanceServiceImpl implements ClearanceService {

    private static final Logger log = LoggerFactory.getLogger(ClearanceServiceImpl.class);

    /** 已完成任务编号保留上限（与前端一致） */
    private static final int MAX_KEEP = 500;
    /** 已完成任务卡快照保留上限（与前端一致） */
    private static final int MAX_RECORDS = 60;

    /** 任务难度 → 权限点（与前端 RANK_POINTS 保持一致） */
    private static final Map<String, Integer> RANK_POINTS =
            Map.of("常规", 1, "中危", 2, "高危", 3, "绝密", 5);

    private final DrUserClearanceMapper mapper;
    private final ObjectMapper objectMapper;

    public ClearanceServiceImpl(DrUserClearanceMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public ClearanceVO get(Long userId) {
        DrUserClearance row = mapper.selectByUserId(userId);
        return row == null ? new ClearanceVO() : fromRow(row);
    }

    @Override
    public ClearanceVO mergeAndSave(Long userId, String account, ClearanceVO incoming) {
        if (userId == null) {
            throw new BizException(401, "登录状态已失效，请重新认证");
        }

        DrUserClearance row = mapper.selectByUserId(userId);
        ClearanceVO base = row == null ? new ClearanceVO() : fromRow(row);
        ClearanceVO in = incoming == null ? new ClearanceVO() : incoming;

        // 任务卡快照按 code 去重，保留 at 较新的一条（先建索引，供计分和输出复用）
        Map<String, Map<String, Object>> byCode = new LinkedHashMap<>();
        putRecords(byCode, base.getRecords());
        putRecords(byCode, in.getRecords());

        // 1) 权限点：base 的基础上，为本设备「新增完成」的编号按难度补点，
        //    再与上报值取较大者。这样两台设备各完成不同任务时会累加，而不是被 max 吞掉。
        Set<String> baseCodes = new HashSet<>();
        addAll(baseCodes, base.getCompleted());
        int gained = 0;
        if (in.getCompleted() != null) {
            for (String code : in.getCompleted()) {
                if (code == null || code.isBlank() || baseCodes.contains(code)) continue;
                gained += rankPointsOf(byCode.get(code));
            }
        }
        int points = Math.max(nz(base.getPoints()) + gained, nz(in.getPoints()));

        // 2) 已完成编号取并集，保序去重
        LinkedHashSet<String> codeSet = new LinkedHashSet<>();
        addAll(codeSet, base.getCompleted());
        addAll(codeSet, in.getCompleted());
        List<String> completed = new ArrayList<>(codeSet);
        if (completed.size() > MAX_KEEP) {
            completed = new ArrayList<>(completed.subList(completed.size() - MAX_KEEP, completed.size()));
        }

        // 3) 快照列表：按 at 倒序，限制条数
        List<Map<String, Object>> records = new ArrayList<>(byCode.values());
        records.sort(Comparator.comparingLong((Map<String, Object> r) -> atOf(r)).reversed());
        if (records.size() > MAX_RECORDS) {
            records = new ArrayList<>(records.subList(0, MAX_RECORDS));
        }

        // 4) 落库（无则插入，有则更新）
        if (row == null) {
            DrUserClearance fresh = new DrUserClearance();
            fresh.setUserId(userId);
            fresh.setAccount(account);
            fresh.setPoints(points);
            fresh.setCompletedJson(toJson(completed));
            fresh.setRecordsJson(toJson(records));
            try {
                mapper.insert(fresh);
            } catch (DuplicateKeyException e) {
                // 多设备同时首次上报：uk_user_id 唯一键兜底，改用更新而不是抛 500
                DrUserClearance exist = mapper.selectByUserId(userId);
                if (exist == null) {
                    throw new BizException(500, "权限进度保存失败，请稍后重试");
                }
                exist.setAccount(account);
                exist.setPoints(points);
                exist.setCompletedJson(toJson(completed));
                exist.setRecordsJson(toJson(records));
                mapper.updateById(exist);
            }
        } else {
            row.setAccount(account);
            row.setPoints(points);
            row.setCompletedJson(toJson(completed));
            row.setRecordsJson(toJson(records));
            mapper.updateById(row);
        }

        log.debug("权限进度已同步 userId={} account={} points={} completed={}",
                userId, account, points, completed.size());
        return new ClearanceVO(points, completed, records);
    }

    /* ---------------- 内部工具 ---------------- */

    private ClearanceVO fromRow(DrUserClearance row) {
        ClearanceVO vo = new ClearanceVO();
        vo.setPoints(nz(row.getPoints()));
        vo.setCompleted(readJson(row.getCompletedJson(), new TypeReference<List<String>>() {
        }, new ArrayList<>()));
        vo.setRecords(readJson(row.getRecordsJson(),
                new TypeReference<List<Map<String, Object>>>() {
                }, new ArrayList<>()));
        return vo;
    }

    private <T> T readJson(String json, TypeReference<T> type, T fallback) {
        if (json == null || json.isBlank()) return fallback;
        try {
            T v = objectMapper.readValue(json, type);
            return v == null ? fallback : v;
        } catch (Exception e) {
            // 历史脏数据不应导致整个接口 500，降级为空
            log.warn("权限进度 JSON 解析失败，已降级为空：{}", e.getMessage());
            return fallback;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new ArrayList<>() : value);
        } catch (Exception e) {
            throw new BizException(500, "权限进度保存失败：数据序列化异常");
        }
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static void addAll(Collection<String> target, List<String> src) {
        if (src == null) return;
        for (String s : src) {
            if (s != null && !s.isBlank()) target.add(s);
        }
    }

    /** 某个已完成任务的难度对应权限点；取不到难度时按「常规」计 1 点 */
    private static int rankPointsOf(Map<String, Object> record) {
        if (record == null) return 1;
        Object rank = record.get("rank");
        if (rank == null) return 1;
        Integer p = RANK_POINTS.get(String.valueOf(rank));
        return p == null ? 1 : p;
    }

    private static void putRecords(Map<String, Map<String, Object>> target, List<Map<String, Object>> src) {
        if (src == null) return;
        for (Map<String, Object> r : src) {
            if (r == null) continue;
            Object code = r.get("code");
            if (code == null || String.valueOf(code).isBlank()) continue;
            String key = String.valueOf(code);
            Map<String, Object> old = target.get(key);
            if (old == null || atOf(r) >= atOf(old)) {
                target.put(key, r);
            }
        }
    }

    private static long atOf(Map<String, Object> r) {
        Object at = r.get("at");
        if (at instanceof Number n) return n.longValue();
        if (at != null) {
            try {
                return Long.parseLong(String.valueOf(at));
            } catch (NumberFormatException ignored) {
                // 忽略非法时间戳
            }
        }
        return 0L;
    }
}
