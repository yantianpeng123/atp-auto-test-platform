package com.atp.module.ci.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CI 集成配置表：一个项目一条记录，保存 CI 令牌（哈希）、默认环境、默认批次与回调地址。
 * 对应 tb_ci_config。
 */
@Data
@TableName("tb_ci_config")
public class CiConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目ID */
    private Long projectId;

    /** CI 令牌哈希（存储 BCrypt 哈希，不保存明文） */
    private String tokenHash;

    /** 默认执行环境ID（触发时未传 envId 时使用） */
    private Long defaultEnvId;

    /** 默认批次ID（触发时未传 batchId 时使用） */
    private Long defaultBatchId;

    /** 批次执行完成后的回调地址（可选，平台主动 POST 结果） */
    private String callbackUrl;

    /** 是否启用：1 启用 0 停用 */
    private Integer enabled;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
