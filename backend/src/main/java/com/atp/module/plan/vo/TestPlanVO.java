package com.atp.module.plan.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试计划列表/详情出参（对齐前端 api/plan.ts 的 TestPlanInfo）。
 *
 * <p>与数据库实体 {@link com.atp.module.plan.entity.TestPlan} 的区别：
 * enabled 转为 Boolean、补充了 envName/caseCount/lastRunTime 等派生字段。
 */
@Data
public class TestPlanVO {

    private Long id;

    private Long projectId;

    private Long envId;

    private String envName;

    private String name;

    private String cron;

    private Boolean enabled;

    private Integer caseCount;

    private Long lastRunId;

    private LocalDateTime lastRunTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
