package com.atp.module.project.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目成员与角色（项目级 RBAC）。
 *
 * <p>{@code role} 存储 {@link com.atp.module.project.enums.ProjectRole} 的枚举名（大写字符串）。
 * 同一 {(project_id, user_id)} 唯一；逻辑删除沿用 {@code deleted}。
 */
@Data
@TableName("tb_project_member")
public class ProjectMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long userId;

    /** OWNER/MAINTAINER/DEVELOPER/VIEWER */
    private String role;

    /** 邀请人用户ID（审计用，出参时转为昵称） */
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
