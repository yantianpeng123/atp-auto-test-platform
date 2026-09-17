package com.atp.module.testcase.mapper;

import com.atp.module.testcase.entity.CaseStep;
import com.atp.module.testcase.vo.CaseStepVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用例步骤数据访问
 */
@Mapper
public interface CaseStepMapper extends BaseMapper<CaseStep> {

    /**
     * 查询用例下的所有步骤（按执行顺序排列，左连接接口定义带出接口信息）
     */
    @Select("""
            SELECT s.id,
                   s.case_id,
                   s.api_id,
                   s.phase,
                   s.step_type,
                   s.component_id,
                   s.generator_id,
                   s.variable_name,
                   s.regen_each_run,
                   s.sort_order,
                   s.step_name,
                   s.request_override,
                   s.assertions,
                   s.response_var,
                   s.is_disabled,
                   s.promote_global,
                   s.continue_on_fail,
                   s.description,
                   a.name   AS api_name,
                   a.method AS api_method,
                   a.path   AS api_path,
                   g.name   AS generator_name
            FROM tb_case_step s
            LEFT JOIN tb_api_definition a ON s.api_id = a.id AND a.deleted = 0
            LEFT JOIN tb_data_generator g ON s.generator_id = g.id AND g.deleted = 0
            WHERE s.deleted = 0
              AND s.case_id = #{caseId}
            ORDER BY FIELD(s.phase, 'pre', 'main', 'post'), s.sort_order ASC
            """)
    List<CaseStepVO> selectStepsByCaseId(@Param("caseId") Long caseId);
}
