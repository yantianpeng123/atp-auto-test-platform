package com.atp.module.plan.mapper;

import com.atp.module.plan.entity.TestPlanCase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 计划-用例关联数据访问
 */
@Mapper
public interface TestPlanCaseMapper extends BaseMapper<TestPlanCase> {
}
