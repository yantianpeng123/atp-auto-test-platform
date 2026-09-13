package com.atp.module.plan.mapper;

import com.atp.module.plan.entity.TestPlan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 测试计划数据访问
 */
@Mapper
public interface TestPlanMapper extends BaseMapper<TestPlan> {
}
