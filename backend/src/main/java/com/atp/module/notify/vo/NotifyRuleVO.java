package com.atp.module.notify.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知规则出参（对齐前端 NotifyRule）
 */
@Data
@Builder
public class NotifyRuleVO {

    private Long id;
    private Long projectId;
    private String event;
    private List<Long> channelIds;
    /** 附加条件 JSON 对象 */
    private Object condition;
    private Boolean enabled;
    private LocalDateTime createTime;
}
