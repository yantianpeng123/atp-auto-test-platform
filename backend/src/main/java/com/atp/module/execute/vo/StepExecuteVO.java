package com.atp.module.execute.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 步骤执行明细。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepExecuteVO {

    private Long stepId;

    /** 所属组合组件ID（组件展开子步骤时填写） */
    private Long componentId;

    /** 父步骤ID（组件展开时为容器步骤ID） */
    private Long parentStepId;

    /** 嵌套层级：0-用例直接步骤 1-组件内 2-嵌套组件内 */
    private Integer nestLevel;

    private String stepName;

    private Integer sortOrder;

    /** 实际请求方法 */
    private String method;

    /** 实际请求 URL */
    private String url;

    /** 实际请求头（JSON 字符串） */
    private String requestHeaders;

    /** 实际请求体（JSON 字符串） */
    private String requestBody;

    private Integer statusCode;

    /** 响应头（JSON 字符串） */
    private String responseHeaders;

    private String responseBody;

    private List<AssertionResultVO> assertResults;

    /** PASSED / FAILED / ERROR */
    private String status;

    private String errorMsg;

    private Long durationMs;
}
