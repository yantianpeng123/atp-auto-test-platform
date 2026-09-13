package com.atp.module.plan.mapper;

import com.atp.module.plan.entity.PlanBatchItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 批次-计划关联 Mapper
 */
@Mapper
public interface PlanBatchItemMapper extends BaseMapper<PlanBatchItem> {
}
