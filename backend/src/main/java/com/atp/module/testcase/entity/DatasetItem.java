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
 * 数据项：保存一行具体数据（JSON 对象，key=字段名，value=值）。
 */
@Data
@TableName("tb_dataset_item")
public class DatasetItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    /** 数据行（JSON 对象字符串，如 {"username":"admin","password":"admin123"}） */
    private String data;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
