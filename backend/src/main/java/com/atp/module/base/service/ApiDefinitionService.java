package com.atp.module.base.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atp.module.base.dto.ApiCreateRequest;
import com.atp.module.base.dto.ApiUpdateRequest;
import com.atp.module.base.entity.ApiDefinition;
import com.atp.module.base.vo.ApiVO;

import java.io.InputStream;

/**
 * 接口定义服务
 */
public interface ApiDefinitionService extends IService<ApiDefinition> {

    /**
     * 接口列表分页查询
     */
    IPage<ApiVO> selectApiPage(long page, long size, Long projectId, Long applicationId,
                               Long versionId, Long moduleId, String name, String path);

    /**
     * 新增接口
     */
    void createApi(ApiCreateRequest request);

    /**
     * 编辑接口
     */
    void updateApi(ApiUpdateRequest request);

    /**
     * 删除接口
     */
    void deleteApi(Long id);

    /**
     * 从 Jar 包导入接口，返回导入数量
     */
    int importApiFromJar(Long moduleId, InputStream jarInputStream);

    /**
     接口唯一性：同一模块下请求方式+请求路径不可重复
     */
    Boolean apIsone(long moduleId, String method, String path);

}

