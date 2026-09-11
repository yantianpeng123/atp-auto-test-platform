package com.atp.module.testcase.mapper;

import com.atp.module.testcase.entity.DatasetItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据项数据访问
 */
@Mapper
public interface DatasetItemMapper extends BaseMapper<DatasetItem> {
}
