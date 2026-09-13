package com.atp.module.plan.service;

import com.atp.common.result.PageResult;
import com.atp.module.plan.dto.PlanExecuteResult;
import com.atp.module.plan.dto.TestPlanForm;
import com.atp.module.plan.vo.TestPlanVO;

/**
 * 测试计划服务
 */
public interface TestPlanService {

    /** 分页列表（按项目隔离；可选名称/启用过滤） */
    PageResult<TestPlanVO> list(Long projectId, String name, Boolean enabled, long page, long size);

    /** 新建计划（同步写入关联用例） */
    void create(TestPlanForm form);

    /** 编辑计划（重建关联用例） */
    void update(TestPlanForm form);

    /** 删除计划（逻辑删除计划，物理删除关联） */
    void delete(Long id);

    /** 启/停用定时 */
    void toggleEnabled(Long id, Boolean enabled);

    /** 按计划执行（循环调用用例执行，汇总结果） */
    PlanExecuteResult executePlan(Long id);
}
