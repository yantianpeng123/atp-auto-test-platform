package com.atp.module.project.controller;

import com.atp.common.exception.BizException;
import com.atp.common.result.Result;
import com.atp.common.result.ResultCode;
import com.atp.module.project.dto.ProjectCreateRequest;
import com.atp.module.project.service.ProjectService;
import com.atp.module.project.vo.ProjectVO;
import com.atp.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private boolean isAdmin(UserPrincipal principal) {
        return ROLE_ADMIN.equals(principal.getRole());
    }
}
