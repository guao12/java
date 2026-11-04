package com.fenglin.springboottest.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 权限进度视图：与前端 utils/clearance.js 的 state 结构保持一致。
 *
 * @author fenglin
 */
public class ClearanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 累计权限点 */
    private Integer points = 0;

    /** 已完成任务编号 */
    private List<String> completed = new ArrayList<>();

    /** 已完成任务卡快照（新的在前） */
    private List<Map<String, Object>> records = new ArrayList<>();

    public ClearanceVO() {
    }

    public ClearanceVO(Integer points, List<String> completed, List<Map<String, Object>> records) {
        this.points = points;
        this.completed = completed;
        this.records = records;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public List<String> getCompleted() {
        return completed;
    }

    public void setCompleted(List<String> completed) {
        this.completed = completed;
    }

    public List<Map<String, Object>> getRecords() {
        return records;
    }

    public void setRecords(List<Map<String, Object>> records) {
        this.records = records;
    }
}
