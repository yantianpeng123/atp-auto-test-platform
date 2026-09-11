package com.atp.module.testcase.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用例步骤入参（嵌套在 CaseCreateRequest / CaseUpdateRequest 中）
 */
@Data
public class StepDTO {

    /** 关联接口ID */
    @NotNull(message = "请选择步骤关联接口")
    private Long apiId;

    /** 执行顺序，从 1 开始 */
    private Integer sortOrder;

    /** 步骤名称 */
    private String stepName;

    /** 请求覆盖内容（JSON 字符串），可引用变量 ${varName} */
    private String requestOverride;

    /** 步骤断言规则（JSON 字符串） */
    private String assertions;

    /** 响应变量名（非空时保存该接口响应数据，供后续步骤引用） */
    private String responseVar;
}
