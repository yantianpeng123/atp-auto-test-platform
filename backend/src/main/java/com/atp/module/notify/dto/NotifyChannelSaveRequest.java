package com.atp.module.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增/编辑通知渠道入参
 */
@Data
public class NotifyChannelSaveRequest {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /** INAPP / DINGTALK / EMAIL_163 */
    @NotBlank(message = "渠道类型不能为空")
    private String type;

    @NotBlank(message = "渠道名称不能为空")
    private String name;

    /** 0-停用 1-启用；为空时默认 1 */
    private Integer enabled;

    /** 渠道配置 JSON 对象；INAPP 可传 null */
    private Object config;
}
