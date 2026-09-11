package com.atp.module.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.atp.module.project.entity.Project;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目数据访问
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
