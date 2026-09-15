package com.atp.module.base.vo;

import com.atp.module.testcase.vo.CaseStepVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组合组件出参（含子步骤）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiComponentVO {

    private Long id;

    private Long projectId;

    private Long moduleId;

    private String name;

    private String description;

    private Long createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 子步骤（含接口信息） */
    private List<CaseStepVO> steps;
}
