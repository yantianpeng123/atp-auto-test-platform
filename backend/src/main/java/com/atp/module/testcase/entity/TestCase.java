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
 * 测试用例
 */
@Data
@TableName("tb_test_case")
public class TestCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    /** 工程ID */
    private Long applicationId;

    /** 版本ID */
    private Long versionId;

    /** 模块ID */
    private Long moduleId;

    /** 关联接口ID，可为空（纯脚本用例） */
    private Long apiId;

    private String name;

    /** 创建人名称 */
    private String creatorName;

    /** 优先级 1-P0 2-P1 3-P2 */
    private Integer level;

    /** 请求内容（JSON 字符串） */
    private String request;

    /** 断言规则（JSON 字符串） */
    private String assertions;

    /** 前置脚本 */
    private String setupScript;

    /** 0-停用 1-启用 */
    private Integer status;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
