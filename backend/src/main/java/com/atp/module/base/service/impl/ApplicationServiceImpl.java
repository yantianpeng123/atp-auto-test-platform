package com.atp.module.base.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.base.dto.ApplicationCreateRequest;
import com.atp.module.base.entity.Application;
import com.atp.module.base.entity.ApplicationModule;
import com.atp.module.base.entity.ApplicationVersion;
import com.atp.module.base.mapper.ApplicationMapper;
import com.atp.module.base.mapper.ApplicationModuleMapper;
import com.atp.module.base.mapper.ApplicationVersionMapper;
import com.atp.module.base.service.ApplicationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工程服务实现
 */
@Service
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application> implements ApplicationService {

    @Autowired
    private ApplicationVersionMapper versionMapper;

    @Autowired
    private ApplicationModuleMapper moduleMapper;

    @Override
    public Application getByName(String name, Long projectId) {
        return lambdaQuery()
                .eq(Application::getName, name)
                .eq(Application::getProjectId, projectId)
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createApplication(ApplicationCreateRequest request) {
        if (getByName(request.getName(), request.getProjectId()) != null) {
            throw new BizException(ResultCode.PROJECT_NAME_EXISTS);
        }
        Application app = new Application();
        app.setProjectId(request.getProjectId());
        app.setName(request.getName());
        app.setDescription(request.getDescription());
        save(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameApplication(Long id, String name, String description) {
        Application app = getById(id);
        if (app == null) {
            throw new BizException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!app.getName().equals(name)) {
            Application exist = getByName(name, app.getProjectId());
            if (exist != null && !exist.getId().equals(id)) {
                throw new BizException(ResultCode.PROJECT_NAME_EXISTS);
            }
        }
        app.setName(name);
        if (description != null) {
            app.setDescription(description);
        }
        updateById(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeApplication(Long id) {
        Application app = getById(id);
        if (app == null) {
            throw new BizException(ResultCode.PROJECT_NOT_FOUND);
        }
        // 级联逻辑删除其版本与模块，避免孤儿数据（直接用 Mapper，避免与子级 Service 形成循环依赖）
        List<ApplicationVersion> versions = versionMapper.selectList(
                new LambdaQueryWrapper<ApplicationVersion>().eq(ApplicationVersion::getApplicationId, id));
        if (!versions.isEmpty()) {
            List<Long> versionIds = versions.stream()
                    .map(ApplicationVersion::getId)
                    .collect(Collectors.toList());
            moduleMapper.delete(new LambdaQueryWrapper<ApplicationModule>().in(ApplicationModule::getVersionId, versionIds));
            versionMapper.delete(new LambdaQueryWrapper<ApplicationVersion>().in(ApplicationVersion::getId, versionIds));
        }
        removeById(id);
    }
}
