package com.atp.module.ci.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * CI 运行记录列表项：按项目列出 CI 触发产生的批次运行实例。
 */
@Data
public class CiRunItem {

    /** 运行实例ID（即批次运行 runId） */
    private Long runId;

    /** 批次ID */
    private Long batchId;

    /** 批次名称 */
    private String batchName;

    /** 触发方式，固定为 CI */
    private String triggerType;

    /** RUNNING / SUCCESS / PARTIAL_FAILED / FAILED */
    private String status;

    private Integer total;
    private Integer passed;
    private Integer failed;

    private LocalDateTime startTime;
    private Long durationMs;
}
