package com.atp.module.base.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atp.module.base.entity.ApplicationVersion;

/**
 * 工程版本服务
 */
public interface ApplicationVersionService extends IService<ApplicationVersion> {

    /**
     * 新增版本（版本名同工程下唯一，工程描述非空则同步更新）
     */
    void createVersion(Long applicationId, String name, String description);
}
