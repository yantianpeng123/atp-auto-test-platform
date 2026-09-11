package com.atp.module.testcase.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用例步骤（一个用例包含多个步骤，串行执行，支持参数传递）
 */
@Data
@TableName("tb_case_step")
public class CaseStep {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long caseId;

    private Long apiId;

    /** 执行顺序，从 1 开始 */
    private Integer sortOrder;

    private String stepName;

    /** 请求覆盖内容（JSON），可引用上一步提取的变量 ${varName} */
    private String requestOverride;

    /** 步骤断言规则（JSON） */
    private String assertions;

    /** 响应变量名（非空时把该接口响应数据存入变量上下文，供后续步骤引用） */
    private String responseVar;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
