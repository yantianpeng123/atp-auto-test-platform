package com.atp.module.plan.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 批次运行明细出参（对齐前端 api/planBatch.ts 的 PlanBatchRunItem）。
 */
@Data
public class PlanBatchRunItemVO {

    private Long id;

    private Long runId;

    private Long planId;

    private String planName;

    private Integer sortOrder;

    /** QUEUED / RUNNING / SUCCESS / FAILED / SKIPPED */
    private String status;

    /** 关联执行记录ID（复用 tb_execution） */
    private Long executionId;

    private Long durationMs;

    private String errorMsg;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
