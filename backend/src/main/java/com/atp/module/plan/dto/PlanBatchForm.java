package com.atp.module.plan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 定时任务批次新建/编辑表单。
 *
 * <p>enabled / failContinue 使用 Boolean 与前端全链路一致（前端为开关布尔值），
 * 落库时再转成 0/1 的 Integer。
 */
@Data
public class PlanBatchForm {

    /** 编辑时必填，新建时为空 */
    private Long id;

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @NotNull(message = "批次名称不能为空")
    private String name;

    /** 执行策略 SERIAL / PARALLEL（默认 SERIAL） */
    @NotNull(message = "执行策略不能为空")
    private String strategy;

    /** 串行时失败后是否继续 */
    private Boolean failContinue;

    /** 并行最大并发数（默认 3） */
    private Integer maxConcurrency;

    /** 关联的测试计划ID列表（顺序即执行顺序） */
    @NotNull(message = "关联计划不能为空")
    private List<Long> planIds;

    /** Cron 表达式（可空，空表示不定时） */
    private String cron;

    /** 0-关闭 1-启用 */
    private Boolean enabled;
}
