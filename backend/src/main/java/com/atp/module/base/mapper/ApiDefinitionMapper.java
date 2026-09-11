package com.atp.module.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.atp.module.base.entity.ApiDefinition;
import com.atp.module.base.vo.ApiVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 接口定义数据访问
 */
@Mapper
public interface ApiDefinitionMapper extends BaseMapper<ApiDefinition> {

    /**
     * 接口列表分页查询（接口 → 模块 → 版本 → 工程 四表联查）
     */
    @Select("""
            SELECT api.id AS id,
                   api.name AS name,
                   api.method AS method,
                   api.path AS path,
                   api.headers AS headers,
                   api.body AS body,
                   api.description AS description,
                   api.source_flag as sourceFlag,
                   m.id AS moduleId,
                   m.name AS moduleName,
                   v.id AS versionId,
                   v.name AS versionName,
                   a.id AS applicationId,
                   a.name AS applicationName,
                   api.update_time AS updateTime
            FROM tb_api_definition api
            JOIN tb_application_module m ON api.module_id = m.id AND m.deleted = 0
            JOIN tb_application_version v ON m.version_id = v.id AND v.deleted = 0
            JOIN tb_application a ON v.application_id = a.id AND a.deleted = 0
            WHERE api.deleted = 0
              AND (#{projectId} IS NULL OR a.project_id = #{projectId})
              AND (#{applicationId} IS NULL OR a.id = #{applicationId})
              AND (#{versionId} IS NULL OR v.id = #{versionId})
              AND (#{moduleId} IS NULL OR m.id = #{moduleId})
              AND (#{name} IS NULL OR api.name LIKE CONCAT('%', #{name}, '%'))
              AND (#{path} IS NULL OR api.path LIKE CONCAT('%', #{path}, '%'))
            ORDER BY api.update_time DESC
            """)
    IPage<ApiVO> selectApiPage(Page<ApiVO> page,
                               @Param("projectId") Long projectId,
                               @Param("applicationId") Long applicationId,
                               @Param("versionId") Long versionId,
                               @Param("moduleId") Long moduleId,
                               @Param("name") String name,
                               @Param("path") String path);
}
