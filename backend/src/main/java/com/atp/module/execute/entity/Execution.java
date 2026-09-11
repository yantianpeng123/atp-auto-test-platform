package com.atp.module.execute.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 执行记录头表：一次执行动作（手动执行=1个用例）的汇总。
 * 对应 tb_execution。
 */
@Data
@TableName("tb_execution")
public class Execution {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划ID（手动执行为 NULL） */
    private Long planId;

    /** 项目ID */
    private Long projectId;

    /** 用例ID（手动执行填） */
    private Long caseId;

    /** 用例名称快照 */
    private String caseName;

    /** 执行环境ID */
    private Long envId;

    /** 执行环境名称快照 */
    private String envName;

    /** 触发类型 MANUAL/SCHEDULED/CI */
    private String triggerType;

    /** 执行人ID */
    private Long executorId;

    /** 状态 RUNNING/SUCCESS/FAILED */
    private String status;

    private Integer totalRounds;
    private Integer passedRounds;
    private Integer failedRounds;
    private Integer totalSteps;
    private Integer passedSteps;
    private Integer failedSteps;

    private Long durationMs;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
