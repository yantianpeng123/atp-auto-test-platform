package com.atp.module.project.enums;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;

/**
 * 项目内角色（项目级 RBAC）。
 *
 * <p>排序即权限层级：{@code OWNER > MAINTAINER > DEVELOPER > VIEWER}。
 * 枚举名直接作为 {@code tb_project_member.role} 的存储值，前后端共用同一份白名单。
 */
public enum ProjectRole {

    /** 拥有者：项目最高权限，可管理成员、转让、删除项目 */
    OWNER,
    /** 维护者：可管理成员与大部分配置，不能删除项目 */
    MAINTAINER,
    /** 开发者：可编辑业务数据（接口/用例/计划），不可管理成员 */
    DEVELOPER,
    /** 访客：只读 */
    VIEWER;

    /**
     * 解析字符串为角色，非法值返回 null（由调用方决定如何处理）。
     */
    public static ProjectRole from(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ProjectRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 判断当前角色权限是否不低于 {@code other}（用于「至少 MAINTAINER 方可管理」之类校验）。
     */
    public boolean isAtLeast(ProjectRole other) {
        return this.ordinal() <= other.ordinal();
    }

    /** 校验角色合法性，非法时抛业务异常 */
    public static ProjectRole require(String value) {
        ProjectRole role = from(value);
        if (role == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "非法项目角色：" + value
                    + "，可用：" + java.util.Arrays.toString(values()));
        }
        return role;
    }
}
