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

    /** 步骤阶段：pre-前置 / main-主步骤 / post-后置，默认 main */
    private String phase;

    /** 步骤类型：1-单接口 2-组合组件 3-生成变量，默认 1 */
    private Integer stepType;

    /** 组合组件ID（stepType=2 时引用，可为空） */
    private Long componentId;

    /** 数据生成器ID（stepType=3 时引用，可为空） */
    private Long generatorId;

    /** 生成值写入的变量名（stepType=3 时必填，后续步骤用 ${name} 引用） */
    private String variableName;

    /** 每轮是否重新生成值：0-否 1-是（stepType=3；当前一律每轮重算，跨轮固定语义未实现） */
    private Integer regenEachRun;

    /** 执行顺序，从 1 开始 */
    private Integer sortOrder;

    private String stepName;

    /** 请求覆盖内容（JSON），可引用上一步提取的变量 ${varName} */
    private String requestOverride;

    /** 步骤断言规则（JSON） */
    private String assertions;

    /** 响应变量名（非空时把该接口响应数据存入变量上下文，供后续步骤引用） */
    private String responseVar;

    /** 是否禁用：0-否 1-是（禁用步骤跳过执行） */
    private Integer isDisabled;

    /** 是否提升为全局变量：0-否 1-是 */
    private Integer promoteGlobal;

    /** 失败后是否继续执行：0-否 1-是 */
    private Integer continueOnFail;

    /** 扩展说明 */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
