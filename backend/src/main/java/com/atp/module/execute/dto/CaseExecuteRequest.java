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
}
