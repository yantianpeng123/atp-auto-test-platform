package com.atp.module.plan.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批次运行实例出参（对齐前端 api/planBatch.ts 的 PlanBatchRun）。
 *
 * <p>计数（passed/failed/running/queued）在返回时按明细实时聚合，
 * 因此前端轮询 getBatchRun 能看到进行中的进度。
 */
@Data
public class PlanBatchRunVO {

    private Long id;

    private Long batchId;

    /** 批次名称（前端运行详情/列表展示用） */
    private String batchName;

    private String triggerType;

    private String status;

    private Integer total;
    private Integer passed;
    private Integer failed;
    private Integer running;
    private Integer queued;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;

    private List<PlanBatchRunItemVO> items;
}
