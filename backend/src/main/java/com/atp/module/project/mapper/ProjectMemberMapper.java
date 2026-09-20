package com.atp.module.project.mapper;

import com.atp.module.project.entity.ProjectMember;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 项目成员数据访问。
 *
 * <p>{@link #selectByProjectAndUser} 为原生查询，刻意绕过 {@code @TableLogic}，
 * 以便「移除后再邀请」时能找回已被逻辑删除的同一行并重新激活（避免唯一键冲突）。
 */
@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {

    @Select("SELECT * FROM tb_project_member WHERE project_id = #{projectId} AND user_id = #{userId} LIMIT 1")
    ProjectMember selectByProjectAndUser(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
