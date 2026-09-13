package com.atp.module.execute.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用例调试执行入参。
 */
@Data
public class CaseExecuteRequest {

    /** 目标环境 ID */
    @NotNull(message = "环境ID不能为空")
    private Long envId;

    /** 是否调试执行：调试运行不落库，仅正式「执行」才记录 */
    private Boolean debug;

    /** 计划ID：由测试计划执行时透传，用于标记 trigger_type=SCHEDULED 并回填 plan_id */
    private Long planId;
}
