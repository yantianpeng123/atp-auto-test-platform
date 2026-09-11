package com.atp.module.dataset.controller;

import com.atp.common.result.PageResult;
import com.atp.common.result.Result;
import com.atp.module.dataset.dto.DatasetTemplateRequest;
import com.atp.module.dataset.service.DatasetService;
import com.atp.module.dataset.vo.DatasetTemplateVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据源（模板 + 数据项）接口。
 */
@RestController
@RequestMapping("/api/dataset")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;

    /** 数据源模板分页查询 */
    @GetMapping("/template/list")
    public Result<PageResult<DatasetTemplateVO>> list(@RequestParam(required = false) Long caseId,
                                                       @RequestParam(required = false) String name,
                                                       @RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size) {
        IPage<DatasetTemplateVO> result = datasetService.selectPage(page, size, caseId, name);
        PageResult<DatasetTemplateVO> pageResult = PageResult.<DatasetTemplateVO>builder()
                .records(result.getRecords())
                .total(result.getTotal())
                .page(result.getCurrent())
                .size(result.getSize())
                .build();
        return Result.success(pageResult);
    }

    /** 数据源模板详情（含字段与数据项） */
    @GetMapping("/template/{id}")
    public Result<DatasetTemplateVO> detail(@PathVariable Long id) {
        return Result.success(datasetService.getDetail(id));
    }

    /** 新增数据源模板（含字段与数据项） */
    @PostMapping("/template")
    public Result<Void> create(@Valid @RequestBody DatasetTemplateRequest request) {
        datasetService.create(request);
        return Result.ok("数据源新增成功");
    }

    /** 编辑数据源模板（含字段与数据项，整体替换） */
    @PutMapping("/template")
    public Result<Void> update(@Valid @RequestBody DatasetTemplateRequest request) {
        datasetService.update(request);
        return Result.ok("数据源修改成功");
    }

    /** 删除数据源模板（级联删除数据项） */
    @DeleteMapping("/template/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        datasetService.delete(id);
        return Result.ok("数据源删除成功");
    }
}
