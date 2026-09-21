package com.atp.module.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知发送日志（留痕，避免静默丢失）。无逻辑删除列，物理保留。
 */
@Data
@TableName("tb_notify_log")
public class NotifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long ruleId;

    private Long channelId;

    @TableField("`event`")
    private String event;

    /** 渠道类型 INAPP/DINGTALK/EMAIL_163，便于前端展示 */
    private String channelType;

    /** 发送目标：webhook url / 收件人 / 站内信用户 */
    private String target;

    /** SUCCESS / FAILED */
    private String status;

    /** 发送内容摘要 */
    private String content;

    /** 失败原因 */
    private String error;

    /** 发送时间，由数据库默认值填充 */
    private LocalDateTime createTime;
}
