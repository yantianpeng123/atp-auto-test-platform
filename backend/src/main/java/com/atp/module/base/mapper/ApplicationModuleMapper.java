package com.atp.module.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.atp.module.base.entity.ApplicationModule;
import com.atp.module.base.vo.VersionInfoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 工程模块数据访问
 */
@Mapper
public interface ApplicationModuleMapper extends BaseMapper<ApplicationModule> {

    /**
     * 工程版本信息分页查询（模块 → 版本 → 工程 三表联查）
     */
    @Select("""
            SELECT m.id AS moduleId,
                   m.name AS moduleName,
                   v.id AS versionId,
                   v.name AS versionName,
                   a.id AS applicationId,
                   a.name AS applicationName,
                   m.update_time AS updateTime
            FROM tb_application_module m
            JOIN tb_application_version v ON m.version_id = v.id AND v.deleted = 0
            JOIN tb_application a ON v.application_id = a.id AND a.deleted = 0
            WHERE m.deleted = 0
              AND (#{projectId} IS NULL OR a.project_id = #{projectId})
              AND (#{applicationId} IS NULL OR a.id = #{applicationId})
              AND (#{versionId} IS NULL OR v.id = #{versionId})
              AND (#{moduleId} IS NULL OR m.id = #{moduleId})
            ORDER BY m.update_time DESC
            """)
    IPage<VersionInfoVO> selectVersionInfoPage(Page<VersionInfoVO> page,
                                               @Param("projectId") Long projectId,
                                               @Param("applicationId") Long applicationId,
                                               @Param("versionId") Long versionId,
                                               @Param("moduleId") Long moduleId);
}
