package com.atp.module.base.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.base.entity.Application;
import com.atp.module.base.entity.ApplicationVersion;
import com.atp.module.base.mapper.ApplicationVersionMapper;
import com.atp.module.base.service.ApplicationService;
import com.atp.module.base.service.ApplicationVersionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
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
}
