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
 * 数据生成器（项目内共享，供 {@code step_type=3} 的「生成变量」步骤引用）
 *
 * <p>{@code params} 在库里是 JSON 列，实体里按 JSON 文本存取——
 * 结构化/反结构化的转换统一放在 Service 层做，实体保持纯 POJO，
 * 避免引入额外的 TypeHandler 依赖。
 */
@Data
@TableName("tb_data_generator")
public class DataGenerator {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String name;

    /** RANDOM/PHONE/IDCARD/NAME/ENUM/TIMESTAMP/UUID/CUSTOM */
    private String type;

    /** JSON 列：结构化参数（随 type），CUSTOM 时为 {@code {template:"..."}} */
    private String params;

    private String description;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
