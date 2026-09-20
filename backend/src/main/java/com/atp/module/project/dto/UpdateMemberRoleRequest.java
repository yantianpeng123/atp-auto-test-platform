package com.atp.module.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改成员角色入参
 */
@Data
public class UpdateMemberRoleRequest {

    /** 成员记录ID */
    @NotNull(message = "成员ID不能为空")
    private Long id;

    /** 新角色：OWNER/MAINTAINER/DEVELOPER/VIEWER */
    @NotBlank(message = "角色不能为空")
    private String role;
}
