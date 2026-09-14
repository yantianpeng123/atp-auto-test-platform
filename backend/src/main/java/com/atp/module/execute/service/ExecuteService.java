package com.atp.module.execute.service;

import com.atp.module.execute.dto.CaseExecuteRequest;
import com.atp.module.execute.vo.CaseExecuteVO;
import com.atp.module.execute.vo.ExecutionReportVO;
import com.atp.module.execute.vo.ExecutionSummaryVO;
import com.atp.common.result.PageResult;

/**
 * 用例执行服务。
 */
public interface ExecuteService {

    /**
     * 单用例调试执行（同步）。
     *
     * @param caseId  用例 ID
     * @param request 执行入参（环境 ID）
     * @return 执行汇总结果
     */
    CaseExecuteVO executeCase(Long caseId, CaseExecuteRequest request);

    /**
     * 查询用例最近一次执行记录（持久化数据）。
     *
     * @param caseId 用例 ID
     * @return 最近一次执行结果；无记录时返回 null
     */
    CaseExecuteVO getLatestExecution(Long caseId);

    /**
     * 按 executionId 查询单次执行报告（含轮次/步骤/断言明细）。
     *
     * @param id 执行记录 ID
     * @return 完整报告 VO
     */
    ExecutionReportVO getExecutionReport(Long id);

    /**
     * 执行记录分页列表（报告中心）。
     * 支持项目隔离、用例/计划名关键字、状态过滤。
     *
     * @param projectId 项目ID（项目隔离；为 null 时不限制）
     * @param keyword   用例名/计划名关键字（模糊）
     * @param status    状态过滤（SUCCESS/FAILED/RUNNING）
     * @param page      页码（从 1）
     * @param size      每页大小
     */
    PageResult<ExecutionSummaryVO> getReportPage(Long projectId, String keyword, String status, long page, long size);
}
