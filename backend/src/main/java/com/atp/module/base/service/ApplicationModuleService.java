package com.atp.module.base.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atp.module.base.entity.ApplicationModule;
import com.atp.module.base.vo.VersionInfoVO;

import java.util.List;

/**
 * 工程模块服务
 */
public interface ApplicationModuleService extends IService<ApplicationModule> {

    /**
     * 批量新增模块（模块名同版本下唯一，工程描述非空则同步更新）
     */
    void createModules(Long versionId, List<String> names, String description);

    /**
     * 工程版本信息分页查询
     */
    IPage<VersionInfoVO> selectVersionInfoPage(long page, long size,
                                               Long projectId, Long applicationId, Long versionId, Long moduleId);
}
