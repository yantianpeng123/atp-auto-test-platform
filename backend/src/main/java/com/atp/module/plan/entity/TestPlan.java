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
 * 测试计划头表：一个计划关联多个用例，可定时执行。
 * 对应 tb_test_plan。
 */
@Data
@TableName("tb_test_plan")
public class TestPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目ID（项目隔离） */
    private Long projectId;

    /** 执行环境ID */
    private Long envId;

    /** 计划名称 */
    private String name;

    /** Cron 表达式（NULL/空表示不定时） */
    private String cron;

    /** 0-关闭 1-启用定时 */
    private Integer enabled;

    /** 最近执行ID */
    private Long lastRunId;

    /** 逻辑删除（全局 logic-delete-field 生效，必须有该字段才能正确隔离） */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
