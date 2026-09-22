package com.atp.module.plan.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 批次运行实例表：一次批次执行的汇总。
 * 对应 tb_plan_batch_run（追加历史，不逻辑删除）。
 */
@Data
@TableName("tb_plan_batch_run")
public class PlanBatchRun {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次ID */
    private Long batchId;

    /** 触发方式 MANUAL / SCHEDULED / CI */
    private String triggerType;

    /** 执行环境ID（CI 触发可覆盖计划默认环境，缺省为 null） */
    private Long envId;

    /** RUNNING / SUCCESS / PARTIAL_FAILED / FAILED */
    private String status;

    private Integer total;
    private Integer passed;
    private Integer failed;
    private Integer running;
    private Integer queued;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
