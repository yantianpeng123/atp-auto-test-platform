package com.atp.module.project.service;

import com.atp.module.project.dto.AddMemberRequest;
import com.atp.module.project.dto.UpdateMemberRoleRequest;
import com.atp.module.project.entity.ProjectMember;
import com.atp.module.project.vo.ProjectMemberVO;
import com.atp.module.user.entity.User;
import com.atp.security.UserPrincipal;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 项目成员与角色服务（项目级 RBAC）。
 *
 * <p>权限模型：
 * <ul>
 *   <li>全局 {@code ADMIN} 视为跨项目超管，可读写任意项目成员；</li>
 *   <li>{@code OWNER}/{@code MAINTAINER} 可管理成员（增删改角色）；</li>
 *   <li>{@code DEVELOPER}/{@code VIEWER} 仅可查看列表；</li>
 *   <li>任一项目至少保留一名 {@code OWNER}（降级/移除最后一名 OWNER 会被拒绝）。</li>
 * </ul>
 */
public interface ProjectMemberService extends IService<ProjectMember> {

    /**
     * 当前用户在指定项目中的角色；非成员或非管理员返回 null。
     *
     * @param isAdmin 调用方已判定的全局管理员标识（为 true 时直接返回 null，前端用 isAdmin 判定权限）
     */
    String getMyRole(Long projectId, Long userId, boolean isAdmin);

    /** 项目成员列表（成员或管理员可见） */
    List<ProjectMemberVO> listMembers(Long projectId, UserPrincipal principal);

    /** 邀请成员加入项目（OWNER/MAINTAINER 或管理员） */
    void addMember(AddMemberRequest request, UserPrincipal principal);

    /** 修改成员角色（OWNER/MAINTAINER 或管理员） */
    void updateMemberRole(UpdateMemberRoleRequest request, UserPrincipal principal);

    /** 移除成员（OWNER/MAINTAINER 或管理员） */
    void removeMember(Long id, UserPrincipal principal);

    /** 将用户登记为项目 OWNER（供新建项目时自动回填，不做权限校验） */
    void addOwner(Long projectId, Long userId);
}
