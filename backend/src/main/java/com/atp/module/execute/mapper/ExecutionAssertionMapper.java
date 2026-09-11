package com.atp.module.execute.mapper;

import com.atp.module.execute.entity.ExecutionAssertion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 断言结果表数据访问。
 */
@Mapper
public interface ExecutionAssertionMapper extends BaseMapper<ExecutionAssertion> {
}
