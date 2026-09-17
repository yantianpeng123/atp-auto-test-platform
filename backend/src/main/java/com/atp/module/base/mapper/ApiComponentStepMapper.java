package com.atp.module.base.mapper;

import com.atp.module.base.entity.ApiComponentStep;
import com.atp.module.testcase.vo.CaseStepVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 组合组件步骤数据访问
 */
@Mapper
public interface ApiComponentStepMapper extends BaseMapper<ApiComponentStep> {

    /**
     * 查询组件下的步骤（按执行顺序，左连接接口定义带出接口信息）。
     * 复用 CaseStepVO 结构，便于执行引擎直接展开执行。
     */
    @Select("""
            SELECT s.id,
                   s.component_id,
                   s.step_type,
                   s.child_component_id AS componentId,
                   s.api_id            AS apiId,
                   s.generator_id,
                   s.variable_name,
                   s.regen_each_run,
                   s.sort_order,
                   s.step_name,
                   s.request_override,
                   s.assertions,
                   s.response_var,
                   s.is_disabled,
                   a.name   AS api_name,
                   a.method AS api_method,
                   a.path   AS api_path,
                   g.name   AS generator_name
            FROM tb_api_component_step s
            LEFT JOIN tb_api_definition a ON s.api_id = a.id AND a.deleted = 0
            LEFT JOIN tb_data_generator g ON s.generator_id = g.id AND g.deleted = 0
            WHERE s.deleted = 0
              AND s.component_id = #{componentId}
            ORDER BY s.sort_order ASC
            """)
    List<CaseStepVO> selectComponentSteps(@Param("componentId") Long componentId);
}
