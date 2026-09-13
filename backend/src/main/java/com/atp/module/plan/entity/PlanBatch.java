package com.atp.module.plan.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务批次头表：一个批次编排多个测试计划，可定时 / 手动批量执行。
 * 对应 tb_plan_batch。
 */
@Data
@TableName("tb_plan_batch")
public class PlanBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目ID（项目隔离） */
    private Long projectId;

    /** 批次名称 */
    private String name;

    /** 执行策略 SERIAL / PARALLEL */
    private String strategy;

    /** 串行时失败后是否继续 0-否 1-是 */
    private Integer failContinue;

    /** 并行最大并发数 */
    private Integer maxConcurrency;

    /** Cron 表达式（NULL/空表示不定时） */
    private String cron;

    /** 0-关闭 1-启用定时 */
    private Integer enabled;

    /** 最近一次运行实例ID */
    private Long lastRunId;

    /** 逻辑删除（全局 logic-delete-field 生效） */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
