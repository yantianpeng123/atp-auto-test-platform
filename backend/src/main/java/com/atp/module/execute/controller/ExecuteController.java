package com.atp.module.execute.controller;

import com.atp.common.result.Result;
import com.atp.common.result.PageResult;
import com.atp.module.execute.dto.CaseExecuteRequest;
import com.atp.module.execute.service.ExecuteService;
import com.atp.module.execute.vo.CaseExecuteVO;
import com.atp.module.execute.vo.ExecutionReportVO;
import com.atp.module.execute.vo.ExecutionSummaryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用例执行接口。
 */
@RestController
@RequestMapping("/api/execute")
@RequiredArgsConstructor
public class ExecuteController {

    private final ExecuteService executeService;

    /** 单用例调试执行 */
    @PostMapping("/case/{caseId}")
    public Result<CaseExecuteVO> executeCase(@PathVariable Long caseId,
                                             @Valid @RequestBody CaseExecuteRequest request) {
        return Result.success(executeService.executeCase(caseId, request));
    }

    /** 查询用例最近一次执行记录（刷新后不丢） */
    @GetMapping("/history/{caseId}")
    public Result<CaseExecuteVO> getHistory(@PathVariable Long caseId) {
        return Result.success(executeService.getLatestExecution(caseId));
    }

    /** 单次执行报告：按 executionId 查询轮次/步骤/断言明细 */
    @GetMapping("/{executionId}")
    public Result<ExecutionReportVO> getReport(@PathVariable Long executionId) {
        return Result.success(executeService.getExecutionReport(executionId));
    }

    /** 执行记录分页列表（报告中心）：项目隔离 + 用例/计划名关键字 + 状态过滤 */
    @GetMapping("/list")
    public Result<PageResult<ExecutionSummaryVO>> getReportList(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(executeService.getReportPage(projectId, keyword, status, page, size));
    }
}
