package com.atp.module.plan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 批次-计划关联表：批次与测试计划的多对多快照（顺序 sort_order 决定执行先后）。
 * 对应 tb_plan_batch_item。
 * <p>注意：该表 <b>无 deleted 列</b>，全局逻辑删除自动跳过，这里即为物理删除（重建关联时清掉旧数据）。
 */
@Data
@TableName("tb_plan_batch_item")
public class PlanBatchItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次ID */
    private Long batchId;

    /** 测试计划ID */
    private Long planId;

    /** 执行顺序 */
    private Integer sortOrder;
}
