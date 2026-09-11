package com.atp.module.testcase.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HAR 导入结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HarImportResult {

    /** 本次新插入的接口定义数量 */
    private int apiCreatedCount;

    /** 跳过（已存在）的接口定义数量 */
    private int apiSkippedCount;

    /** 生成的用例 ID */
    private Long caseId;

    /** 生成的步骤数量 */
    private int stepCount;
}
