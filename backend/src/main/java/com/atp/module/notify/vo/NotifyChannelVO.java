package com.atp.module.notify.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知渠道出参（对齐前端 NotifyChannel）
 */
@Data
@Builder
public class NotifyChannelVO {

    private Long id;
    private Long projectId;
    private String type;
    private String name;
    private Boolean enabled;
    /** 解析后的配置 JSON 对象（DINGTALK/EMAIL_163）；INAPP 为 null */
    private Object config;
    private LocalDateTime createTime;
}
