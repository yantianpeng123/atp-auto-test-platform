package com.atp.module.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 新增通知规则入参
 */
@Data
public class NotifyRuleSaveRequest {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /** EXEC_DONE / EXEC_FAIL / BATCH_DONE */
    @NotBlank(message = "触发事件不能为空")
    private String event;

    /** 命中的渠道ID列表 */
    @NotEmpty(message = "至少选择一个渠道")
    private List<Long> channelIds;

    /** 附加条件 JSON 对象，如 {"onlyFail":true}；可空 */
    private Object condition;

    /** 0-停用 1-启用；为空时默认 1 */
    private Integer enabled;
}
