package com.atp.module.plan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 测试计划新建/编辑表单。
 *
 * <p>enabled 使用 Boolean 与前端保持全链路一致（前端 UI 为开关布尔值），
 * 落库时再转成 0/1 的 Integer。
 */
@Data
public class TestPlanForm {

    /** 编辑时必填，新建时为空 */
    private Long id;

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @NotNull(message = "计划名称不能为空")
    private String name;

    @NotNull(message = "环境ID不能为空")
    private Long envId;

    @NotNull(message = "关联用例不能为空")
    private List<Long> caseIds;

    /** Cron 表达式（可空，空表示不定时） */
    private String cron;

    /** 0-关闭 1-启用（前端布尔值） */
    private Boolean enabled;
}
