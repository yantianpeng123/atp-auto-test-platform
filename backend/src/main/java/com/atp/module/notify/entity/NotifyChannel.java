package com.atp.module.notify.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知渠道（项目级）：钉钉 Webhook / 163 邮件 / 站内信。
 *
 * <p>{@code config} 为 JSON 列，实体按字符串存取，结构化解构统一在 Service 层做。
 */
@Data
@TableName("tb_notify_channel")
public class NotifyChannel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    /** INAPP / DINGTALK / EMAIL_163 */
    @TableField("`type`")
    private String type;

    private String name;

    /** JSON 字符串：DINGTALK=webhook/secret/atMobiles；EMAIL_163=host/port/username/authCode/from/ssl/to；INAPP 无 */
    private String config;

    /** 0-停用 1-启用 */
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
