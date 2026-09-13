package com.atp.module.plan.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务批次列表/详情出参（对齐前端 api/planBatch.ts 的 PlanBatchInfo）。
 *
 * <p>与数据库实体 {@link com.atp.module.plan.entity.PlanBatch} 的区别：
 * enabled / failContinue 转为 Boolean、补充了 lastRunTime / lastRunStatus 等派生字段。
 */
@Data
public class PlanBatchVO {

    private Long id;

    private Long projectId;

    private String name;

    private String strategy;

    private Boolean failContinue;

    private Integer maxConcurrency;

    private String cron;

    private Boolean enabled;

    private Long lastRunId;

    private LocalDateTime lastRunTime;

    private String lastRunStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
