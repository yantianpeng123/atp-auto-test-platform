package com.atp.module.plan.dto;

import lombok.Data;

import java.util.List;

/**
 * 计划执行汇总结果（对齐前端 api/plan.ts 的 PlanExecuteResult）。
 */
@Data
public class PlanExecuteResult {

    private Long planId;

    private String planName;

    private int totalCases;

    private int passedCases;

    private int failedCases;

    private long durationMs;

    private List<PlanExecuteCaseResult> cases;

    /** 单个用例的执行结果（对齐前端 PlanExecuteCaseResult） */
    @Data
    public static class PlanExecuteCaseResult {

        private Long caseId;

        private String caseName;

        private String status;

        private long durationMs;

        private int passedSteps;

        private int failedSteps;
    }
}
