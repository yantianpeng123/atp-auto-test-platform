package com.atp.module.ci.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * CI 运行记录分页结果，附带按状态聚合的汇总计数（不受分页影响，覆盖该项目全部数据）。
 */
@Data
@Builder
public class CiRunsPageVO {

    /** 当前页的运行记录列表 */
    private List<CiRunItem> records;

    /** 全部数据总条数 */
    private long total;

    /** 当前页码 */
    private long page;

    /** 每页大小 */
    private long size;

    /** 按状态聚合的汇总计数（覆盖全部数据，非当前页） */
    private CiRunSummary summary;

    /** 按状态聚合的汇总计数 */
    @Data
    @Builder
    public static class CiRunSummary {

        /** 全部运行总数 */
        private long total;

        /** 成功 */
        private long success;

        /** 部分失败 */
        private long partialFailed;

        /** 失败 */
        private long failed;

        /** 运行中 */
        private long running;
    }
}
