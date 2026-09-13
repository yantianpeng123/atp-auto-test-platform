package com.atp.module.plan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 计划-用例关联表：一个计划关联多个用例，sort_order 决定执行顺序。
 * 对应 tb_plan_case。
 *
 * <p>注意：该表<b>没有</b> deleted 列。MyBatis-Plus 全局逻辑删除只对「实体中存在
 * deleted 属性」的类生效；本实体刻意不定义该字段，全局逻辑删除会自动跳过它，
 * 因此对此表的增删都是物理操作（关联快照物理删除是期望行为）。
 */
@Data
@TableName("tb_plan_case")
public class TestPlanCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划ID */
    private Long planId;

    /** 用例ID */
    private Long caseId;

    /** 执行顺序（拖拽/排序决定） */
    private Integer sortOrder;
}
