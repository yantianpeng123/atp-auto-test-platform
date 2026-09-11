package com.atp.module.dataset.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据源模板出参（含字段 keys 与数据项 items）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetTemplateVO {

    private Long id;

    private Long caseId;

    private String caseName;

    private String name;

    /** 字段定义（JSON 数组字符串） */
    private String keys;

    private String creatorName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<DatasetItemVO> items;
}
