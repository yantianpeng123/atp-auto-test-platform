package com.atp.module.testcase.controller;

import com.atp.common.result.PageResult;
import com.atp.common.result.Result;
import com.atp.module.testcase.dto.CaseCreateRequest;
import com.atp.module.testcase.dto.CaseUpdateRequest;
import com.atp.module.testcase.service.CaseService;
import com.atp.module.testcase.service.HarImportResult;
import com.atp.module.testcase.vo.CaseStepVO;
import com.atp.module.testcase.vo.CaseVO;
import com.atp.security.UserPrincipal;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 测试用例接口
 */
@RestController
@RequestMapping("/api/case")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    /** 用例分页查询 */
    @GetMapping("/list")
    public Result<PageResult<CaseVO>> list(@RequestParam(required = false) Long projectId,
                                           @RequestParam(required = false) Long apiId,
                                           @RequestParam(required = false) String name,
                                           @RequestParam(required = false) Integer level,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        IPage<CaseVO> result = caseService.selectCasePage(page, size, projectId, apiId, name, level, status);
        PageResult<CaseVO> pageResult = PageResult.<CaseVO>builder()
                .records(result.getRecords())
                .total(result.getTotal())
                .page(result.getCurrent())
                .size(result.getSize())
                .build();
        return Result.success(pageResult);
    }

    /** 用例详情（含步骤列表） */
    @GetMapping("/{id}")
    public Result<CaseVO> detail(@PathVariable Long id) {
        return Result.success(caseService.getCaseDetail(id));
    }

    /** 查询用例步骤列表 */
    @GetMapping("/{caseId}/steps")
    public Result<List<CaseStepVO>> steps(@PathVariable Long caseId) {
        return Result.success(caseService.getCaseSteps(caseId));
    }

    /** 新增用例 */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CaseCreateRequest request) {
        caseService.createCase(request, currentPrincipal().getId());
        return Result.ok("用例新增成功");
    }

    /** 编辑用例 */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody CaseUpdateRequest request) {
        caseService.updateCase(request);
        return Result.ok("用例修改成功");
    }

    /** 删除用例 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        caseService.deleteCase(id);
        return Result.ok("用例删除成功");
    }

    /** 启用 / 停用（status：0-停用 1-启用） */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        caseService.updateStatus(id, status);
        return Result.ok(status != null && status == 1 ? "用例已启用" : "用例已停用");
    }

    /**
     * HAR 包导入用例
     *
     * <p>multipart 字段：har（文件）、moduleId、caseName
     */
    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public Result<HarImportResult> importHar(@RequestParam Long moduleId,
                                             @RequestParam String caseName,
                                             @RequestPart("har") MultipartFile har) {
        HarImportResult result = caseService.importHar(moduleId, caseName, har, currentPrincipal().getId());
        String msg = String.format("导入成功：新增接口 %d 个，跳过 %d 个，生成用例 1 个，步骤 %d 个",
                result.getApiCreatedCount(), result.getApiSkippedCount(), result.getStepCount());
        return Result.success(msg, result);
    }

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
