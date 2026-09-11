package com.atp.module.env.mapper;

import com.atp.module.env.entity.TestEnv;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 测试环境数据访问
 */
@Mapper
public interface TestEnvMapper extends BaseMapper<TestEnv> {
}
