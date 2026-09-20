package com.atp.module.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.atp.module.project.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 项目数据访问
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    /**
     * 查询指定用户可见的项目列表（一次性 JOIN 成员表）。
     *
     * <p>普通用户可见范围 = 其作为活跃成员（tb_project_member.deleted=0）的项目，
     * 兼容 RBAC 上线前无成员行的老项目（owner_id = 当前用户 同样可见）。
     * 原生 SQL 不走 MyBatis-Plus 的 @TableLogic 自动拼接，故 p.deleted 与 pm.deleted 需手动过滤。
     */
    @Select("""
            SELECT DISTINCT p.id, p.name, p.team, p.owner_id, p.create_by,
                   p.create_time, p.update_time, p.deleted
            FROM tb_project p
            LEFT JOIN tb_project_member pm
                   ON pm.project_id = p.id AND pm.deleted = 0 AND pm.user_id = #{userId}
            WHERE p.deleted = 0
              AND (pm.id IS NOT NULL OR p.owner_id = #{userId})
            ORDER BY p.id ASC
            """)
    List<Project> listVisibleByUser(@Param("userId") Long userId);
}
