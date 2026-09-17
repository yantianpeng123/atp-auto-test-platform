package com.atp.module.base.mapper;

import com.atp.module.base.entity.DataGenerator;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据生成器数据访问
 */
@Mapper
public interface DataGeneratorMapper extends BaseMapper<DataGenerator> {
}
