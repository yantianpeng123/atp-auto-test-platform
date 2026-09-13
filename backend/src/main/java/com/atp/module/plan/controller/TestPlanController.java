package com.atp.module.plan.controller;

import com.atp.common.result.PageResult;
import com.atp.common.result.Result;
import com.atp.module.plan.dto.PlanExecuteResult;
import com.atp.module.plan.dto.TestPlanForm;
import com.atp.module.plan.service.TestPlanService;
import com.atp.module.plan.vo.TestPlanVO;
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
 * 测试计划接口（对接前端 /api/plan/**）
 */
@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class TestPlanController {

    private final TestPlanService testPlanService;

    /** 计划分页列表 */
    @GetMapping("/list")
    public Result<PageResult<TestPlanVO>> list(@RequestParam(required = false) Long projectId,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) Boolean enabled,
                                              @RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size) {
        return Result.success(testPlanService.list(projectId, name, enabled, page, size));
    }

    /** 新建计划 */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody TestPlanForm form) {
        testPlanService.create(form);
        return Result.ok("计划创建成功");
    }

    /** 编辑计划 */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody TestPlanForm form) {
        testPlanService.update(form);
        return Result.ok("计划更新成功");
    }

    /** 删除计划 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        testPlanService.delete(id);
        return Result.ok("计划删除成功");
    }

    /** 启/停用计划 */
    @PutMapping("/{id}/enabled")
    public Result<Void> toggleEnabled(@PathVariable Long id, @RequestParam Boolean enabled) {
        testPlanService.toggleEnabled(id, enabled);
        return Result.ok(Boolean.TRUE.equals(enabled) ? "计划已启用" : "计划已停用");
    }

    /** 按计划执行 */
    @PostMapping("/{id}/execute")
    public Result<PlanExecuteResult> execute(@PathVariable Long id) {
        return Result.success(testPlanService.executePlan(id));
    }
}
