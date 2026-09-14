package com.atp.module.execute.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单次执行报告（按 executionId 查询）。
 * 字段与前端 api/execute.ts 的 ExecutionReport 类型一一对应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionReportVO {

    private Long executionId;

    /** 计划ID（手动执行为 NULL） */
    private Long planId;

    /** 所属计划名称（关联 tb_test_plan 取回，可能为 null） */
    private String planName;

    private Long caseId;
    private String caseName;

    private Long envId;
    private String envName;

    /** MANUAL / SCHEDULED / CI */
    private String triggerType;

    /** 执行人ID */
    private Long executorId;

    /** 执行人名称（关联 sys_user 取回，可能为 null） */
    private String executorName;

    /** RUNNING / SUCCESS / FAILED */
    private String status;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Long durationMs;

    private Integer totalRounds;
    private Integer passedRounds;
    private Integer failedRounds;

    /** 按轮次分组：轮内为步骤明细 */
    private List<RoundExecuteVO> rounds;
}
