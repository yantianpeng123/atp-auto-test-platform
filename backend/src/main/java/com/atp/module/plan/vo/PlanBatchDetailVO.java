package com.atp.module.plan.vo;

import lombok.Data;

import java.util.List;

/**
 * 批次详情出参：在 {@link PlanBatchVO} 基础上补充「关联计划列表」。
 * 用于批次详情页的编排配置展示。
 */
@Data
public class PlanBatchDetailVO extends PlanBatchVO {

    /** 关联计划（按 sortOrder 升序） */
    private List<PlanBrief> plans;

    /** 关联计划简档 */
    @Data
    public static class PlanBrief {
        private Long id;
        private String name;
        private Integer sortOrder;
    }
}
