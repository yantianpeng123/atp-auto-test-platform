package com.atp.module.testcase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增用例入参
 */
@Data
public class CaseCreateRequest {

    @NotNull(message = "请选择所属项目")
    private Long projectId;

    @NotNull(message = "请选择工程")
    private Long applicationId;

    @NotNull(message = "请选择版本")
    private Long versionId;

    @NotNull(message = "请选择模块")
    private Long moduleId;

    @NotBlank(message = "请输入用例名称")
    @Size(max = 200, message = "用例名称最长 200 个字符")
    private String name;

    /** 创建人名称 */
    private String creatorName;

    /** 优先级 1-P0 2-P1 3-P2，默认 P1 */
    private Integer level;

    /** 前置脚本（整个用例执行前运行） */
    private String setupScript;

    /** 0-停用 1-启用，默认启用 */
    private Integer status;

    /** 用例步骤列表（串行执行） */
    @Valid
    private List<StepDTO> steps;

    // ---- 兼容旧的单接口模式（可选，steps 优先） ----

    /** 关联接口，可为空（纯脚本用例） */
    private Long apiId;

    /** 请求内容，JSON 字符串 */
    private String request;

    /** 断言规则，JSON 字符串 */
    private String assertions;
}
