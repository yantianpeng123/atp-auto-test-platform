package com.atp.module.testcase.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用例步骤出参（含关联接口的简要信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseStepVO {

    private Long id;

    private Long caseId;

    private Long apiId;

    /** 步骤阶段：pre-前置 / main-主步骤 / post-后置 */
    private String phase;

    /** 步骤类型：1-单接口 2-组合组件 3-其他类型 */
    private Integer stepType;

    /** 组合组件ID（stepType=2 时引用） */
    private Long componentId;

    /** 关联接口名称 */
    private String apiName;

    /** 关联接口请求方法 */
    private String apiMethod;

    /** 关联接口路径 */
    private String apiPath;

    /** 执行顺序 */
    private Integer sortOrder;

    private String stepName;

    /** 请求覆盖内容（JSON 字符串） */
    private String requestOverride;

    /** 步骤断言规则（JSON 字符串） */
    private String assertions;

    /** 响应变量名 */
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
