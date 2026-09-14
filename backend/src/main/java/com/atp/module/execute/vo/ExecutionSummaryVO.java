package com.atp.module.execute.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 报告中心列表项（执行记录概要）。
 * 字段与前端 api/execute.ts 的 ExecutionSummary 类型一一对应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionSummaryVO {

    private Long executionId;

    private Long planId;

    /** 所属计划名称（可能为 null） */
    private String planName;

    private Long caseId;
    private String caseName;

    private Long envId;
    private String envName;

    /** MANUAL / SCHEDULED / CI */
    private String triggerType;

    /** 执行人名称（可能为 null） */
    private String executorName;

    /** RUNNING / SUCCESS / FAILED */
    private String status;

    private LocalDateTime startTime;

    private Long durationMs;

    private Integer totalRounds;
    private Integer passedRounds;
    private Integer failedRounds;
}
