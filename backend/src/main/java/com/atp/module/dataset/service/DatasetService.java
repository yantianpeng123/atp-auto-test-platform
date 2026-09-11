package com.atp.module.dataset.service;

import com.atp.module.dataset.dto.DatasetTemplateRequest;
import com.atp.module.dataset.vo.DatasetTemplateVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 数据源（模板 + 数据项）服务。
 */
public interface DatasetService {

    IPage<DatasetTemplateVO> selectPage(long page, long size, Long caseId, String name);

    DatasetTemplateVO getDetail(Long id);

    void create(DatasetTemplateRequest request);

    void update(DatasetTemplateRequest request);

    void delete(Long id);
}
