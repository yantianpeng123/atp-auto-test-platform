package com.atp.module.project.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.project.dto.ProjectCreateRequest;
import com.atp.module.project.entity.Project;
import com.atp.module.project.mapper.ProjectMapper;
import com.atp.module.project.service.ProjectMemberService;
import com.atp.module.project.service.ProjectService;
import com.atp.module.project.vo.ProjectVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 项目服务实现
 */
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final ProjectMemberService projectMemberService;

    @Override
    public List<ProjectVO> listProjects(Long userId, boolean isAdmin) {
        List<Project> list;
        if (isAdmin) {
            list = lambdaQuery().orderByAsc(Project::getId).list();
        } else {
            // 普通用户：通过成员表 JOIN 一次性查出可见项目（含 owner 本人兜底），
            // 不再依赖单列 owner_id，使项目列表与 tb_project_member 的 RBAC 关系一致。
            list = baseMapper.listVisibleByUser(userId);
        }
        return list.stream()
                .map(p -> ProjectVO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .team(p.getTeam())
                        .ownerId(p.getOwnerId())
                        .build())
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProject(ProjectCreateRequest request, Long userId) {
        long count = lambdaQuery().eq(Project::getName, request.getName()).count();
        if (count > 0) {
            throw new BizException(ResultCode.PROJECT_NAME_EXISTS, "项目名称已存在");
        }
        Project project = new Project();
        project.setName(request.getName());
        project.setTeam(request.getTeam());
        project.setOwnerId(userId);
        project.setCreateBy(userId);
        save(project);
        // 创建者自动成为项目 OWNER（项目级 RBAC 落地前兼容：否则新项目无成员可管理）
        projectMemberService.addOwner(project.getId(), userId);
    }
}
