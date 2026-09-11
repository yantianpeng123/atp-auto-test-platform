package com.atp.module.project.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.project.dto.ProjectCreateRequest;
import com.atp.module.project.entity.Project;
import com.atp.module.project.mapper.ProjectMapper;
import com.atp.module.project.service.ProjectService;
import com.atp.module.project.vo.ProjectVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 项目服务实现
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    @Override
    public List<ProjectVO> listProjects(Long userId, boolean isAdmin) {
        List<Project> list;
        if (isAdmin) {
            list = lambdaQuery().orderByAsc(Project::getId).list();
        } else {
            list = lambdaQuery().eq(Project::getOwnerId, userId).orderByAsc(Project::getId).list();
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
    }
}
