package com.atp.module.base.service;

import com.atp.common.result.PageResult;
import com.atp.module.base.dto.DataGeneratorSaveRequest;
import com.atp.module.base.vo.DataGeneratorVO;

/**
 * 数据生成器服务
 */
public interface DataGeneratorService {

    /** 分页查询（按项目 / 名称 / 类型过滤） */
    PageResult<DataGeneratorVO> selectGeneratorPage(long page, long size, Long projectId, String name, String type);

    /** 详情 */
    DataGeneratorVO getGenerator(Long id);

    /** 新增，返回保存后的完整信息 */
    DataGeneratorVO createGenerator(DataGeneratorSaveRequest request, Long userId);

    /** 编辑，返回保存后的完整信息 */
    DataGeneratorVO updateGenerator(DataGeneratorSaveRequest request);

    /** 删除（逻辑删除） */
    void deleteGenerator(Long id);
}
