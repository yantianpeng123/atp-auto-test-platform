package com.atp.module.project.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.project.dto.AddMemberRequest;
import com.atp.module.project.dto.UpdateMemberRoleRequest;
import com.atp.module.project.entity.ProjectMember;
import com.atp.module.project.enums.ProjectRole;
import com.atp.module.project.mapper.ProjectMemberMapper;
import com.atp.module.project.mapper.ProjectMapper;
import com.atp.module.project.service.ProjectMemberService;
import com.atp.module.project.vo.ProjectMemberVO;
import com.atp.module.user.entity.User;
import com.atp.module.user.mapper.UserMapper;
import com.atp.security.UserPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目成员与角色服务实现。
 *
 * <p>为避免与 {@code ProjectService} 形成构造器循环依赖，本类直接注入
 * {@code ProjectMapper}/{@code UserMapper}（DAO 层）做存在性校验，而非依赖 Service 层。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl extends ServiceImpl<ProjectMemberMapper, ProjectMember>
        implements ProjectMemberService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;

    @Override
    public String getMyRole(Long projectId, Long userId, boolean isAdmin) {
        if (isAdmin) {
            // 全局管理员无「项目特定角色」，前端用 isAdmin 判定权限，这里返回 null 即可
            return null;
        }
        ProjectMember member = baseMapper.selectByProjectAndUser(projectId, userId);
        return (member != null && isActive(member)) ? member.getRole() : null;
    }

    @Override
    public List<ProjectMemberVO> listMembers(Long projectId, UserPrincipal principal) {
        requireProject(projectId);
        // 查看：项目成员或管理员
        if (!isAdmin(principal) && !isMember(projectId, principal.getId())) {
            throw new BizException(ResultCode.FORBIDDEN, "您不是该项目成员，无法查看成员列表");
        }
        List<ProjectMember> list = lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .orderByAsc(ProjectMember::getId)
                .list();
        return list.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMember(AddMemberRequest request, UserPrincipal principal) {
        Long projectId = request.getProjectId();
        requireProject(projectId);
        assertManageable(projectId, principal);

        User user = userMapper.selectOne(new QueryWrapper<User>()
                .eq("username", request.getUsername().trim())
                .eq("deleted", 0));
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND, "用户不存在：" + request.getUsername());
        }
        ProjectRole role = ProjectRole.require(request.getRole());

        ProjectMember existing = baseMapper.selectByProjectAndUser(projectId, user.getId());
        if (existing != null && isActive(existing)) {
            throw new BizException(ResultCode.BAD_REQUEST, "该用户已是项目成员");
        }
        if (existing != null) {
            // 曾被移除，重新激活并刷新角色/邀请人
            existing.setRole(role.name());
            existing.setCreateBy(principal.getId());
            existing.setDeleted(0);
            existing.setCreateTime(LocalDateTime.now());
            baseMapper.updateById(existing);
            return;
        }
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(user.getId());
        member.setRole(role.name());
        member.setCreateBy(principal.getId());
        baseMapper.insert(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberRole(UpdateMemberRoleRequest request, UserPrincipal principal) {
        ProjectMember member = requireMember(request.getId());
        Long projectId = member.getProjectId();
        assertManageable(projectId, principal);

        ProjectRole newRole = ProjectRole.require(request.getRole());
        // 守护：最后一个 OWNER 不可降级
        if (ProjectRole.OWNER.name().equals(member.getRole())
                && newRole != ProjectRole.OWNER
                && countOwners(projectId) <= 1) {
            throw new BizException(ResultCode.FORBIDDEN, "项目至少需保留一名 OWNER");
        }
        member.setRole(newRole.name());
        baseMapper.updateById(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long id, UserPrincipal principal) {
        ProjectMember member = requireMember(id);
        Long projectId = member.getProjectId();
        assertManageable(projectId, principal);
        // 守护：最后一个 OWNER 不可移除
        if (ProjectRole.OWNER.name().equals(member.getRole()) && countOwners(projectId) <= 1) {
            throw new BizException(ResultCode.FORBIDDEN, "项目至少需保留一名 OWNER");
        }
        baseMapper.deleteById(id); // 逻辑删除
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOwner(Long projectId, Long userId) {
        ProjectMember existing = baseMapper.selectByProjectAndUser(projectId, userId);
        if (existing != null) {
            if (isActive(existing)) {
                return; // 已是活跃成员
            }
            existing.setRole(ProjectRole.OWNER.name());
            existing.setDeleted(0);
            existing.setCreateTime(LocalDateTime.now());
            baseMapper.updateById(existing);
            return;
        }
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(ProjectRole.OWNER.name());
        member.setCreateBy(userId);
        baseMapper.insert(member);
    }

    // ==================== 内部辅助 ====================

    private ProjectMember requireMember(Long id) {
        ProjectMember member = baseMapper.selectById(id);
        if (member == null || !isActive(member)) {
            throw new BizException(ResultCode.NOT_FOUND, "成员记录不存在");
        }
        return member;
    }

    private void requireProject(Long projectId) {
        if (projectMapper.selectById(projectId) == null) {
            throw new BizException(ResultCode.PROJECT_NOT_FOUND, "项目不存在");
        }
    }

    private boolean isMember(Long projectId, Long userId) {
        ProjectMember member = baseMapper.selectByProjectAndUser(projectId, userId);
        return member != null && isActive(member);
    }

    /** 权限断言：管理员放行；否则需为 OWNER/MAINTAINER */
    private void assertManageable(Long projectId, UserPrincipal principal) {
        if (isAdmin(principal)) {
            return;
        }
        String role = getMyRole(projectId, principal.getId(), false);
        if (role == null || !ProjectRole.valueOf(role).isAtLeast(ProjectRole.MAINTAINER)) {
            throw new BizException(ResultCode.FORBIDDEN, "仅项目 OWNER 或 MAINTAINER 可管理成员");
        }
    }

    private long countOwners(Long projectId) {
        return lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getRole, ProjectRole.OWNER.name())
                .count();
    }

    private boolean isActive(ProjectMember member) {
        return member.getDeleted() == null || member.getDeleted() == 0;
    }

    private ProjectMemberVO toVO(ProjectMember member) {
        User user = userMapper.selectById(member.getUserId());
        String inviterName = null;
        if (member.getCreateBy() != null) {
            User inviter = userMapper.selectById(member.getCreateBy());
            if (inviter != null) {
                inviterName = inviter.getNickname();
            }
        }
        return ProjectMemberVO.builder()
                .id(member.getId())
                .projectId(member.getProjectId())
                .userId(member.getUserId())
                .username(user != null ? user.getUsername() : "")
                .nickname(user != null ? user.getNickname() : "")
                .role(member.getRole())
                .inviterName(inviterName)
                .createTime(member.getCreateTime())
                .build();
    }

    private boolean isAdmin(UserPrincipal principal) {
        return ROLE_ADMIN.equals(principal.getRole());
    }
}
