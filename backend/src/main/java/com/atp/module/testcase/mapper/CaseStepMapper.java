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
                   s.sort_order,
                   s.step_name,
                   s.request_override,
                   s.assertions,
                   s.response_var AS responseVar,
                   a.name   AS api_name,
                   a.method AS api_method,
                   a.path   AS api_path
            FROM tb_case_step s
            LEFT JOIN tb_api_definition a ON s.api_id = a.id AND a.deleted = 0
            WHERE s.deleted = 0
              AND s.case_id = #{caseId}
            ORDER BY s.sort_order ASC
            """)
    List<CaseStepVO> selectStepsByCaseId(@Param("caseId") Long caseId);
}
