package com.atp.module.project.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目成员出参
 */
@Data
@Builder
public class ProjectMemberVO {

    private Long id;

    private Long projectId;

    private Long userId;

    private String username;

    private String nickname;

    /** 项目内角色：OWNER/MAINTAINER/DEVELOPER/VIEWER */
    private String role;

    /** 邀请人昵称（展示用，可为空） */
    private String inviterName;

    private LocalDateTime createTime;
}
