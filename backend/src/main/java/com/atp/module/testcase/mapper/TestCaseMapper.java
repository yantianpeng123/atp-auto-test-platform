package com.atp.module.testcase.mapper;

import com.atp.module.testcase.entity.TestCase;
import com.atp.module.testcase.vo.CaseVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 测试用例数据访问
 */
@Mapper
public interface TestCaseMapper extends BaseMapper<TestCase> {

    /**
     * 用例列表分页查询（左连接工程/版本/模块/接口定义，带出各名称）
     *
     * <p>说明：这里跨模块读取工程/版本/模块/接口定义属于刻意的读模型例外——
     * 用例列表必须展示所属工程/版本/模块，否则只有 ID 对使用者没有意义。
     * 写操作仍严格限定在 tb_test_case 单表。
     */
    @Select("""
            SELECT c.id,
                   c.project_id,
                   c.application_id,
                   c.version_id,
                   c.module_id,
                   c.api_id,
                   c.name,
                   c.creator_name,
                   c.level,
                   c.request,
                   c.assertions,
                   c.setup_script,
                   c.status,
                   c.create_by,
                   c.create_time,
                   c.update_time,
                   app.name AS application_name,
                   v.name   AS version_name,
                   m.name   AS module_name,
                   a.name   AS api_name,
                   a.method AS api_method,
                   a.path   AS api_path
            FROM tb_test_case c
            LEFT JOIN tb_application app         ON c.application_id = app.id AND app.deleted = 0
            LEFT JOIN tb_application_version v   ON c.version_id     = v.id   AND v.deleted = 0
            LEFT JOIN tb_application_module m    ON c.module_id      = m.id   AND m.deleted = 0
            LEFT JOIN tb_api_definition a        ON c.api_id         = a.id   AND a.deleted = 0
            WHERE c.deleted = 0
              AND (#{projectId} IS NULL OR c.project_id = #{projectId})
              AND (#{apiId} IS NULL OR c.api_id = #{apiId})
              AND (#{name} IS NULL OR c.name LIKE CONCAT('%', #{name}, '%'))
              AND (#{level} IS NULL OR c.level = #{level})
              AND (#{status} IS NULL OR c.status = #{status})
            ORDER BY c.update_time DESC
            """)
    IPage<CaseVO> selectCasePage(Page<CaseVO> page,
                                 @Param("projectId") Long projectId,
                                 @Param("apiId") Long apiId,
                                 @Param("name") String name,
                                 @Param("level") Integer level,
                                 @Param("status") Integer status);
}
