package com.atp.module.base.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.base.entity.Application;
import com.atp.module.base.entity.ApplicationModule;
import com.atp.module.base.entity.ApplicationVersion;
import com.atp.module.base.mapper.ApplicationModuleMapper;
import com.atp.module.base.service.ApplicationModuleService;
import com.atp.module.base.service.ApplicationService;
import com.atp.module.base.service.ApplicationVersionService;
import com.atp.module.base.vo.VersionInfoVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工程模块服务实现
 */
@Service
@RequiredArgsConstructor
public class ApplicationModuleServiceImpl extends ServiceImpl<ApplicationModuleMapper, ApplicationModule>
        implements ApplicationModuleService {

    private final ApplicationService applicationService;
    private final ApplicationVersionService versionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createModules(Long versionId, List<String> names, String description) {
        ApplicationVersion version = versionService.getById(versionId);
        if (version == null) {
            throw new BizException(ResultCode.VERSION_NOT_FOUND);
        }

        // 工程描述非空则同步更新所属工程
        if (StringUtils.hasText(description)) {
            applicationService.lambdaUpdate()
                    .eq(Application::getId, version.getApplicationId())
                    .set(Application::getDescription, description)
                    .update();
        }

        for (String raw : names) {
            String name = raw.trim();
            if (name.isEmpty()) {
                continue;
            }
            long count = lambdaQuery()
                    .eq(ApplicationModule::getVersionId, versionId)
                    .eq(ApplicationModule::getName, name)
                    .count();
            if (count > 0) {
                throw new BizException(ResultCode.MODULE_NAME_EXISTS);
            }
            ApplicationModule module = new ApplicationModule();
            module.setVersionId(versionId);
            module.setName(name);
            save(module);
        }
    }

    @Override
    public IPage<VersionInfoVO> selectVersionInfoPage(long page, long size,
                                                      Long projectId, Long applicationId, Long versionId, Long moduleId) {
        Page<VersionInfoVO> pageParam = new Page<>(page, size);
        return baseMapper.selectVersionInfoPage(pageParam, projectId, applicationId, versionId, moduleId);
    }
}
