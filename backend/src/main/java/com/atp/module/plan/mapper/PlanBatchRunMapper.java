package com.atp.module.plan.mapper;

import com.atp.module.ci.vo.CiRunItem;
import com.atp.module.ci.vo.CiRunStatusCount;
import com.atp.module.plan.entity.PlanBatchRun;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 批次运行实例 Mapper
 */
@Mapper
public interface PlanBatchRunMapper extends BaseMapper<PlanBatchRun> {

    /**
     * 按项目分页列出 CI 触发产生的运行实例，JOIN tb_plan_batch 取批次名称与项目归属。
     */
    @Select("""
            SELECT r.id AS run_id, r.batch_id AS batch_id, b.name AS batch_name,
                   r.trigger_type AS trigger_type, r.status, r.total, r.passed, r.failed,
                   r.start_time AS start_time, r.duration_ms AS duration_ms
            FROM tb_plan_batch_run r
            LEFT JOIN tb_plan_batch b ON b.id = r.batch_id AND b.deleted = 0
            WHERE b.project_id = #{projectId} AND r.trigger_type = 'CI'
            ORDER BY r.id DESC
            """)
    IPage<CiRunItem> selectCiRuns(Page<CiRunItem> page, @Param("projectId") Long projectId);

    /**
     * 按项目聚合 CI 触发产生的运行实例各状态条数（覆盖全部数据，不受分页影响）。
     */
    @Select("""
            SELECT r.status AS status, COUNT(*) AS cnt
            FROM tb_plan_batch_run r
            LEFT JOIN tb_plan_batch b ON b.id = r.batch_id AND b.deleted = 0
            WHERE b.project_id = #{projectId} AND r.trigger_type = 'CI'
            GROUP BY r.status
            """)
    List<CiRunStatusCount> countCiRunsByStatus(@Param("projectId") Long projectId);
}
