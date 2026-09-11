package com.atp.module.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增项目入参
 */
@Data
public class ProjectCreateRequest {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称最长 100 个字符")
    private String name;

    @Size(max = 100, message = "团队名称最长 100 个字符")
    private String team;
}
