package com.atp.module.dataset.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 数据源模板入参（新增/编辑，含字段 keys 与数据项 items，整体提交）。
 */
@Data
public class DatasetTemplateRequest {

    /** 编辑时必填；新增时为空 */
    private Long id;

    @NotNull(message = "请选择关联用例")
    private Long caseId;

    @NotBlank(message = "请输入数据源名称")
    private String name;

    private String creatorName;

    /** 字段定义（JSON 数组字符串，如 [{"key":"username","desc":"用户名"}]） */
    private String keys;

    /** 数据项（整体替换该模板下的全部数据项） */
    @Valid
    private List<DatasetItemDTO> items;
}
