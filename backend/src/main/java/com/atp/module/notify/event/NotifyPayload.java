package com.atp.module.notify.event;

import lombok.Data;

/**
 * 通知事件载荷：贯穿「事件发布 → 派发 → 各发送器」全链路。
 *
 * <p>标题(title)与正文(content)在 NotifyService.dispatch 阶段统一组装一次，
 * 各发送器直接复用（钉钉走 markdown、邮件走纯文本、站内信走纯文本）。
 */
@Data
public class NotifyPayload {

    private Long projectId;

    /** 触发事件：EXEC_DONE / EXEC_FAIL / BATCH_DONE */
    private String event;

    private Long executionId;

    private String caseName;

    /** 执行结果 SUCCESS / FAILED（用于 onlyFail 条件判断） */
    private String status;

    private Integer totalRounds;
    private Integer passedRounds;
    private Integer failedRounds;

    private Long executorId;
    private String executorName;

    /** 点击跳转的报告/详情 URL */
    private String linkUrl;

    /** 组装后的展示标题 */
    private String title;

    /** 组装后的展示正文（markdown / 纯文本通用） */
    private String content;
}
