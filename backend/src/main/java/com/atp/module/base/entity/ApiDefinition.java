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
 * 接口定义
 */
@Data
@TableName("tb_api_definition")
public class ApiDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long moduleId;

    private String name;

    private String method;

    private String path;

    private String headers;

    private String body;

    private String description;

    private Long createBy;
    //手动添加、Jar导入
    private String sourceFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
