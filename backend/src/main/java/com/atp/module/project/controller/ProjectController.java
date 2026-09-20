package com.atp.module.project.controller;

import com.atp.common.exception.BizException;
import com.atp.common.result.Result;
import com.atp.common.result.ResultCode;
import com.atp.module.project.dto.AddMemberRequest;
import com.atp.module.project.dto.ProjectCreateRequest;
import com.atp.module.project.dto.UpdateMemberRoleRequest;
import com.atp.module.project.service.ProjectMemberService;
import com.atp.module.project.service.ProjectService;
import com.atp.module.project.vo.ProjectMemberVO;
import com.atp.module.project.vo.ProjectVO;
import com.atp.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目接口
 */
@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectController {

    private static final String ROLE_ADMIN = "ADMIN";

    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    /** 当前用户可用项目列表 */
    @GetMapping("/list")
    public Result<List<ProjectVO>> list() {
        UserPrincipal principal = currentPrincipal();
        return Result.success(projectService.listProjects(principal.getId(), isAdmin(principal)));
    }

    /** 新增项目（仅管理员） */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ProjectCreateRequest request) {
        UserPrincipal principal = currentPrincipal();
        if (!isAdmin(principal)) {
            throw new BizException(ResultCode.FORBIDDEN, "仅管理员可新增项目");
        }
        projectService.createProject(request, principal.getId());
        return Result.ok("项目新增成功");
    }

    /** 当前登录用户在指定项目中的角色（侧边栏显隐、权限指令依赖） */
    @GetMapping("/my-role")
    public Result<String> myRole(@RequestParam Long projectId) {
        UserPrincipal principal = currentPrincipal();
        return Result.success(projectMemberService.getMyRole(projectId, principal.getId(), isAdmin(principal)));
    }

    /** 项目成员列表（成员或管理员可见） */
    @GetMapping("/members")
    public Result<List<ProjectMemberVO>> members(@RequestParam Long projectId) {
        UserPrincipal principal = currentPrincipal();
        return Result.success(projectMemberService.listMembers(projectId, principal));
    }

    /** 邀请成员加入项目（OWNER/MAINTAINER 或管理员） */
    @PostMapping("/member")
    public Result<Void> addMember(@Valid @RequestBody AddMemberRequest request) {
        projectMemberService.addMember(request, currentPrincipal());
        return Result.ok("成员已添加");
    }

    /** 修改成员角色（OWNER/MAINTAINER 或管理员） */
    @PutMapping("/member/role")
    public Result<Void> updateMemberRole(@Valid @RequestBody UpdateMemberRoleRequest request) {
        projectMemberService.updateMemberRole(request, currentPrincipal());
        return Result.ok("角色已更新");
    }

    /** 移除成员（OWNER/MAINTAINER 或管理员） */
    @DeleteMapping("/member/{id}")
    public Result<Void> removeMember(@PathVariable Long id) {
        projectMemberService.removeMember(id, currentPrincipal());
        return Result.ok("成员已移除");
    }

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private boolean isAdmin(UserPrincipal principal) {
        return ROLE_ADMIN.equals(principal.getRole());
    }
}
