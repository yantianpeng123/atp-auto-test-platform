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
}
