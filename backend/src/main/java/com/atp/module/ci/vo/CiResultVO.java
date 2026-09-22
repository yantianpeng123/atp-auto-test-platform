package com.atp.module.ci.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * CI 轮询返回：供外部 CI 判断 build 红绿。
 *
 * <p>status 取值：RUNNING（进行中，需继续轮询）/ SUCCESS（全部通过）/
 * PARTIAL_FAILED（部分失败）/ FAILED（全部失败或执行异常）。
 */
@Data
@Builder
public class CiResultVO {

    private Long runId;
    private Long batchId;
    private String status;

    private Integer total;
    private Integer passed;
    private Integer failed;
    private Integer running;
    private Integer queued;

    /** 平台侧报告页地址（供人工查看明细） */
    private String summaryUrl;

    /** 各计划执行明细（可选，便于排错） */
    private List<Object> items;
}
