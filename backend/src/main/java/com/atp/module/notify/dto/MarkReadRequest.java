package com.atp.module.notify.dto;

import lombok.Data;

import java.util.List;

/**
 * 站内信标记已读入参：all=true 时标记当前用户该项目全部；否则按 ids 标记
 */
@Data
public class MarkReadRequest {

    private Long projectId;

    private List<Long> ids;

    private boolean all;
}
