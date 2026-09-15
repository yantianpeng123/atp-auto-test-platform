package com.atp.module.base.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组合组件步骤（组件内可包含单接口步骤，也可嵌套另一个组件）
 */
@Data
@TableName("tb_api_component_step")
public class ApiComponentStep {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long componentId;

    /** 步骤类型：1-单接口 2-嵌套组件 */
    private Integer stepType;

    /** 关联接口ID（stepType=1 时必填） */
    private Long apiId;

    /** 嵌套组件ID（stepType=2 时引用） */
    private Long childComponentId;

    private Integer sortOrder;

    private String stepName;

    /** 请求覆盖内容（JSON） */
    private String requestOverride;

    /** 步骤断言规则（JSON） */
    private String assertions;

    /** 响应变量名 */
    private String responseVar;

    /** 是否禁用：0-否 1-是 */
    private Integer isDisabled;

    /** 失败后是否继续执行：0-否 1-是 */
    private Integer continueOnFail;

    /** 步骤说明 */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
