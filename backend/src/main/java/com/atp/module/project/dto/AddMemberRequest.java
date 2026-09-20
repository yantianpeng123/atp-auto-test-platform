package com.atp.module.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 邀请成员入参
 */
@Data
public class AddMemberRequest {

    /** 项目ID */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /** 被邀请人用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 项目内角色：OWNER/MAINTAINER/DEVELOPER/VIEWER */
    @NotBlank(message = "角色不能为空")
    private String role;
}
