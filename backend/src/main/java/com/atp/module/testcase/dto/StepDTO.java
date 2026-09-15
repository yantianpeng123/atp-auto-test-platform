package com.atp.module.testcase.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用例步骤入参（嵌套在 CaseCreateRequest / CaseUpdateRequest 中）
 */
@Data
public class StepDTO {

    /** 关联接口ID（stepType=1 单接口时必填；组合组件/其他类型可空） */
    private Long apiId;

    /** 步骤阶段：pre-前置 / main-主步骤 / post-后置，默认 main */
    private String phase;

    /** 步骤类型：1-单接口 2-组合组件 3-其他类型，默认 1 */
    private Integer stepType;

    /** 组合组件ID（stepType=2 时引用） */
    private Long componentId;

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

    /** 是否禁用：0-否 1-是 */
    private Integer isDisabled;

    /** 是否提升为全局变量：0-否 1-是 */
    private Integer promoteGlobal;

    /** 失败后是否继续执行：0-否 1-是 */
    private Integer continueOnFail;

    /** 扩展说明 */
    private String description;
}
