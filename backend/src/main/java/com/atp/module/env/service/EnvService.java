package com.atp.module.env.service;

import com.atp.module.env.dto.EnvCreateRequest;
import com.atp.module.env.dto.EnvUpdateRequest;
import com.atp.module.env.vo.EnvVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 测试环境服务
 */
public interface EnvService {

    /** 环境分页查询（按项目过滤，可按名称模糊匹配） */
    IPage<EnvVO> selectEnvPage(long page, long size, Long projectId, String name);

    /** 新增环境 */
    void createEnv(EnvCreateRequest request);

    /** 编辑环境 */
    void updateEnv(EnvUpdateRequest request);

    /** 删除环境（逻辑删除） */
    void deleteEnv(Long id);
}
