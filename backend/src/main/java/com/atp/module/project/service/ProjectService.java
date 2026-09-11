package com.atp.module.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atp.module.project.dto.ProjectCreateRequest;
import com.atp.module.project.entity.Project;
import com.atp.module.project.vo.ProjectVO;

import java.util.List;

/**
 * 项目服务
 */
public interface ProjectService extends IService<Project> {

    /**
     * 查询当前用户可用的项目列表（管理员返回全部，其余返回自己拥有的）
     */
    List<ProjectVO> listProjects(Long userId, boolean isAdmin);

    /**
     * 新增项目
     */
    void createProject(ProjectCreateRequest request, Long userId);
}
