package com.atp.module.execute.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 单轮执行结果（参数化多轮执行时，每一轮对应一个）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundExecuteVO {

    /** 轮次，从 1 开始 */
    private Integer roundIndex;

    /** 本轮参数（参数化数据行，key=变量名） */
    private Map<String, Object> params;

    private Integer passedSteps;

    private Integer failedSteps;

    /** SUCCESS / FAILED */
    private String status;

    private Long durationMs;

    private List<StepExecuteVO> steps;
}
