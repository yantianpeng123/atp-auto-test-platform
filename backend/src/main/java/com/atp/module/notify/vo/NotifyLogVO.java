package com.atp.module.notify.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知发送日志出参（对齐前端 NotifyLog）
 */
@Data
@Builder
public class NotifyLogVO {

    private Long id;
    private Long projectId;
    private String event;
    private String channelType;
    private String target;
    private String status;
    private String content;
    private String error;
    private LocalDateTime createTime;
}
