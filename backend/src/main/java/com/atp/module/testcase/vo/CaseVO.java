package com.atp.module.testcase.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用例出参（含关联接口的简要信息 + 步骤列表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseVO {

    private Long id;

    private Long projectId;

    private Long applicationId;

    private Long versionId;

    private Long moduleId;

    /** 工程名称 */
    private String applicationName;

    /** 版本名称 */
    private String versionName;

    /** 模块名称 */
    private String moduleName;

    private Long apiId;

    /** 关联接口名称，未关联时为 null */
    private String apiName;

    /** 关联接口请求方法 */
    private String apiMethod;

    /** 关联接口路径 */
    private String apiPath;

    private String name;

    /** 创建人名称 */
    private String creatorName;

    /** 优先级 1-P0 2-P1 3-P2 */
    private Integer level;

    /** 请求内容（JSON 字符串，兼容旧模式） */
    private String request;

    /** 断言规则（JSON 字符串，兼容旧模式） */
    private String assertions;

    /** 前置脚本 */
    private String setupScript;

    /** 0-停用 1-启用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 用例步骤列表（多接口串行，含参数提取/引用） */
    private List<CaseStepVO> steps;
}
