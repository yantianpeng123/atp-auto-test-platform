package com.atp.module.plan.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 批次运行明细表：本次运行每个计划的执行结果（execution_id 复用 tb_execution）。
 * 对应 tb_plan_batch_run_item（追加历史，不逻辑删除）。
 */
@Data
@TableName("tb_plan_batch_run_item")
public class PlanBatchRunItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 运行实例ID */
    private Long runId;

    /** 计划ID */
    private Long planId;

    /** 计划名称快照 */
    private String planName;

    /** 顺序快照 */
    private Integer sortOrder;

    /** QUEUED / RUNNING / SUCCESS / FAILED / SKIPPED */
    private String status;

    /** 关联执行记录ID（复用 tb_execution，点开看完整报告） */
    private Long executionId;

    /** 耗时毫秒 */
    private Long durationMs;

    /** 失败原因 */
    private String errorMsg;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
