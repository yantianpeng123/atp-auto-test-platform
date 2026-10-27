package com.atp.module.base.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.base.entity.Application;
import com.atp.module.base.entity.ApplicationModule;
import com.atp.module.base.entity.ApplicationVersion;
import com.atp.module.base.mapper.ApplicationVersionMapper;
import com.atp.module.base.service.ApplicationModuleService;
import com.atp.module.base.service.ApplicationService;
import com.atp.module.base.service.ApplicationVersionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 工程版本服务实现
 */
@Service
@RequiredArgsConstructor
public class ApplicationVersionServiceImpl extends ServiceImpl<ApplicationVersionMapper, ApplicationVersion>
        implements ApplicationVersionService {

    private final ApplicationService applicationService;

    @Autowired
    private ApplicationModuleService moduleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVersion(Long applicationId, String name, String description) {
        Application app = applicationService.getById(applicationId);
        if (app == null) {
            throw new BizException(ResultCode.PROJECT_NOT_FOUND);
        }

        long count = lambdaQuery()
                .eq(ApplicationVersion::getApplicationId, applicationId)
                .eq(ApplicationVersion::getName, name)
                .count();
        if (count > 0) {
            throw new BizException(ResultCode.VERSION_NAME_EXISTS);
        }

        // 工程描述非空则同步更新
        if (StringUtils.hasText(description)) {
            applicationService.lambdaUpdate()
                    .eq(Application::getId, applicationId)
                    .set(Application::getDescription, description)
                    .update();
        }

        ApplicationVersion version = new ApplicationVersion();
        version.setApplicationId(applicationId);
        version.setName(name);
        save(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameVersion(Long id, String name, Long applicationId, String description) {
        ApplicationVersion version = getById(id);
        if (version == null) {
            throw new BizException(ResultCode.VERSION_NOT_FOUND);
        }
        Long targetAppId = (applicationId != null) ? applicationId : version.getApplicationId();
        // 校验归属工程存在
        Application app = applicationService.getById(targetAppId);
        if (app == null) {
            throw new BizException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!version.getName().equals(name)) {
            long count = lambdaQuery()
                    .eq(ApplicationVersion::getApplicationId, targetAppId)
                    .eq(ApplicationVersion::getName, name)
                    .ne(ApplicationVersion::getId, id)
                    .count();
            if (count > 0) {
                throw new BizException(ResultCode.VERSION_NAME_EXISTS);
            }
        }
        version.setName(name);
        if (applicationId != null && !applicationId.equals(version.getApplicationId())) {
            version.setApplicationId(applicationId);
        }
        updateById(version);

        // 工程描述非空则同步更新归属工程（与新增版本语义一致）
        if (StringUtils.hasText(description)) {
            applicationService.lambdaUpdate()
                    .eq(Application::getId, targetAppId)
                    .set(Application::getDescription, description)
                    .update();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeVersion(Long id) {
        ApplicationVersion version = getById(id);
        if (version == null) {
            throw new BizException(ResultCode.VERSION_NOT_FOUND);
        }
        // 级联逻辑删除该版本下的模块，避免孤儿数据
        moduleService.lambdaUpdate()
                .eq(ApplicationModule::getVersionId, id)
                .remove();
        removeById(id);
    }
}
