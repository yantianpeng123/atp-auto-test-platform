package com.atp.module.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内信收件箱（INAPP 渠道的落库目标）。无逻辑删除列，物理保留。
 */
@Data
@TableName("tb_notify_message")
public class NotifyMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收用户ID */
    private Long userId;

    private Long projectId;

    private String title;

    private String content;

    /** 0-未读 1-已读 */
    private Integer read;

    /** 点击跳转的报告/详情 URL */
    private String linkUrl;

    /** 创建时间，由数据库默认值填充 */
    private LocalDateTime createTime;
}
