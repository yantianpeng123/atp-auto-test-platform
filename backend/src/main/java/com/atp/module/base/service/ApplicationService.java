package com.atp.module.base.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atp.module.base.dto.ApplicationCreateRequest;
import com.atp.module.base.entity.Application;

/**
 * 工程服务
 */
public interface ApplicationService extends IService<Application> {

    /**
     * 按名称查询工程
     */
    Application getByName(String name,Long projectId);

    /**
     * 新增工程（校验名称唯一）
     */
    void createApplication(ApplicationCreateRequest request);

    /**
     * 重命名工程（同项目下名称唯一），可同时更新描述
     */
    void renameApplication(Long id, String name, String description);

    /**
     * 删除工程（级联逻辑删除其版本与模块）
     */
    void removeApplication(Long id);
}
