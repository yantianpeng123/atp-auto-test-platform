package com.atp.module.base.service;

import com.atp.common.result.PageResult;
import com.atp.module.base.dto.ApiComponentCreateRequest;
import com.atp.module.base.vo.ApiComponentVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 组合组件服务
 */
public interface ApiComponentService {

    /** 组合组件分页查询（按项目/模块/名称过滤） */
    PageResult<ApiComponentVO> selectComponentPage(long page, long size, Long projectId,
                                                  Long moduleId, String name);

    /** 组件详情（含子步骤） */
    ApiComponentVO getComponentDetail(Long id);

    /** 新增组件 */
    void createComponent(ApiComponentCreateRequest request, Long userId);

    /** 编辑组件（含子步骤替换 + 防环校验） */
    void updateComponent(ApiComponentCreateRequest request);

    /** 删除组件（逻辑删除组件及其子步骤） */
    void deleteComponent(Long id);
}
