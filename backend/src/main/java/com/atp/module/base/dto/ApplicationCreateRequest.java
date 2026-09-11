package com.atp.module.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增工程入参
 */
@Data
public class ApplicationCreateRequest {

    @NotNull(message = "请选择项目")
    private Long projectId;

    @NotBlank(message = "工程名称不能为空")
    @Size(max = 100, message = "工程名称最长 100 个字符")
    private String name;

    @Size(max = 500, message = "工程描述最长 500 个字符")
    private String description;
}
