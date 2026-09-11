package com.atp.module.execute.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用例执行汇总结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseExecuteVO {

    private Long caseId;

    private String caseName;

    private Long envId;

    private Integer totalRounds;

    private Integer passedRounds;

    private Integer failedRounds;

    /** SUCCESS / FAILED */
    private String status;

    private Long durationMs;

    private List<RoundExecuteVO> rounds;
}
