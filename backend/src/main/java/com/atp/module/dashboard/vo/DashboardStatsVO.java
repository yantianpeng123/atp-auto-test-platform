package com.atp.module.dashboard.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 工作台统计卡片数据（全平台聚合）
 */
@Data
public class DashboardStatsVO implements Serializable {

    /** 项目总数（逻辑删除自动排除） */
    private long projectCount;

    /** 用例总数（逻辑删除自动排除） */
    private long caseCount;

    /** 本月执行次数（create_time >= 当月1号 00:00） */
    private long monthExecutionCount;

    /**
     * 通过率（百分比，保留 1 位小数）
     * <p>口径：SUCCESS ÷ (SUCCESS + FAILED)，排除 RUNNING 中等状态。
     * 如 85.7 表示 85.7%。无已完成执行时为 0。
     */
    private double passRate;
}
