package com.atp.module.plan.service;

import com.atp.common.result.PageResult;
import com.atp.module.plan.dto.PlanBatchForm;
import com.atp.module.plan.vo.PlanBatchDetailVO;
import com.atp.module.plan.vo.PlanBatchRunVO;
import com.atp.module.plan.vo.PlanBatchVO;

import java.util.List;

/**
 * 定时任务批次服务
 */
public interface PlanBatchService {

    /** 批次分页列表（按项目隔离；可选名称/启用过滤） */
    PageResult<PlanBatchVO> list(Long projectId, String name, Boolean enabled, long page, long size);

    /** 新建批次（同步写入关联计划） */
    void create(PlanBatchForm form);

    /** 编辑批次（重建关联计划） */
    void update(PlanBatchForm form);

    /** 删除批次（逻辑删除批次，物理删除关联） */
    void delete(Long id);

    /** 启/停用定时 */
    void toggleEnabled(Long id, Boolean enabled);

    /**
     * 执行批次：循环/并发调用既有 {@code TestPlanService.executePlan(planId)}，
     * 落库运行实例与明细，返回实时运行结果。
     *
     * @param id          批次ID
     * @param triggerType 触发方式 MANUAL / SCHEDULED
     */
    PlanBatchRunVO executeBatch(Long id, String triggerType);

    /** 轮询：获取某次运行的实时状态（按明细实时聚合计数） */
    PlanBatchRunVO getRun(Long runId);

    /** 某批次的运行历史（按时间倒序） */
    List<PlanBatchRunVO> getRuns(Long batchId);

    /** 批次详情（含关联计划列表） */
    PlanBatchDetailVO getDetail(Long id);
}
