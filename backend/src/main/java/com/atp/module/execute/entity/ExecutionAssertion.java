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
 * 断言结果表：每条断言一行，归属某一步骤。
 * 对应 tb_execution_assertion。
 */
@Data
@TableName("tb_execution_assertion")
public class ExecutionAssertion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 执行记录ID（冗余，便于单表统计） */
    private Long executionId;

    /** 执行明细ID */
    private Long detailId;

    /** 轮次 */
    private Integer roundIndex;

    /** 步骤序号 */
    private Integer stepIndex;

    /** 断言类型 status/jsonPath/header/body */
    private String type;

    /** jsonPath 或响应头名 */
    private String path;

    /** 比较操作符 eq/notEq/contains/exists */
    private String operator;

    /** 期望值 */
    private String expected;

    /** 实际值 */
    private String actual;

    /** 是否通过 0-否 1-是 */
    private Integer passed;

    /** 失败原因 */
    private String message;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
