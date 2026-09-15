package com.atp.module.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 组合组件新增/编辑入参
 */
@Data
public class ApiComponentCreateRequest {

    /** 编辑时必填 */
    private Long id;

    @NotNull(message = "请选择所属项目")
    private Long projectId;

    /** 所属模块（可选，用于模块树定位） */
    private Long moduleId;

    @NotBlank(message = "请输入组件名称")
    @jakarta.validation.constraints.Size(max = 100, message = "组件名称最长 100 个字符")
    private String name;

    private String description;

    /** 子步骤列表 */
    private List<ComponentStepDTO> steps;

    /** 嵌套组件步骤入参 */
    @Data
    public static class ComponentStepDTO {

        private Long id;

        /** 步骤类型：1-单接口 2-嵌套组件 */
        private Integer stepType;

        /** 关联接口ID（stepType=1 时必填） */
        private Long apiId;

        /** 嵌套组件ID（stepType=2 时引用） */
        private Long childComponentId;

        private Integer sortOrder;

        private String stepName;

        private String requestOverride;

        private String assertions;

        private String responseVar;

        private Integer isDisabled;

        private Integer continueOnFail;

        private String description;
    }
}
