package com.atp.module.ci.vo;

import lombok.Data;

/**
 * CI 运行记录按状态分组聚合的单项（状态 + 该状态条数）。
 */
@Data
public class CiRunStatusCount {

    /** 运行状态：SUCCESS / PARTIAL_FAILED / FAILED / RUNNING */
    private String status;

    /** 该状态的运行实例条数 */
    private long cnt;
}
