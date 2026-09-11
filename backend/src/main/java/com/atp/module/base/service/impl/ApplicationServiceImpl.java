package com.atp.module.base.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.base.dto.ApplicationCreateRequest;
import com.atp.module.base.entity.Application;
import com.atp.module.base.mapper.ApplicationMapper;
import com.atp.module.base.service.ApplicationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工程服务实现
 */
@Service
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application> implements ApplicationService {

    @Override
    public Application getByName(String name,Long projectId ) {
        return lambdaQuery().
                eq(Application::getName, name)
                .eq(Application::getProjectId,projectId)
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createApplication(ApplicationCreateRequest request) {
        if (getByName(request.getName(),request.getProjectId()) != null) {
            throw new BizException(ResultCode.PROJECT_NAME_EXISTS);
        }
        Application app = new Application();
        app.setProjectId(request.getProjectId());
        app.setName(request.getName());
        app.setDescription(request.getDescription());
        save(app);
    }
}
