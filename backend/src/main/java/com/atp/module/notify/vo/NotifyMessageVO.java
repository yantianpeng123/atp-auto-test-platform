package com.atp.module.notify.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内信收件箱出参（对齐前端 NotifyMessage）
 */
@Data
@Builder
public class NotifyMessageVO {

    private Long id;
    private Long userId;
    private Long projectId;
    private String title;
    private String content;
    @JsonProperty("Isread")
    private Boolean isread;
    private String linkUrl;
    private LocalDateTime createTime;
}
