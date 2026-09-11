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
 * 数据源模板：只保存字段名列表（keys），具体数据在数据项表。
 */
@Data
@TableName("tb_dataset_template")
public class DatasetTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long caseId;

    private String name;

    /** 字段名列表（JSON 数组字符串，如 ["username","password"]） */
    @TableField("`keys`")
    private String keys;

    private String creatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
