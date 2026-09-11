package com.atp.module.dataset.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据项出参。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetItemVO {

    private Long id;

    private Long templateId;

    /** 数据行（JSON 对象字符串） */
    private String data;

    private Integer sortOrder;
}
