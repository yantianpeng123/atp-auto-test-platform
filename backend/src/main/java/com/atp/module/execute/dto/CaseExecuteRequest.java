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
}
