package com.atp.module.dataset.dto;

import lombok.Data;

/**
 * 数据项入参（嵌套在数据源模板新增/编辑中）。
 */
@Data
public class DatasetItemDTO {

    /** 数据行（JSON 对象字符串，key=字段名，value=值） */
    private String data;

    /** 执行顺序 */
    private Integer sortOrder;
}
