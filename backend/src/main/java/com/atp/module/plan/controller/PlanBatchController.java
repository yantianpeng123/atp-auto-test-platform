package com.atp.module.plan.controller;

import com.atp.common.result.PageResult;
import com.atp.common.result.Result;
import com.atp.module.plan.dto.PlanBatchForm;
import com.atp.module.plan.service.PlanBatchService;
import com.atp.module.plan.vo.PlanBatchDetailVO;
import com.atp.module.plan.vo.PlanBatchRunVO;
import com.atp.module.plan.vo.PlanBatchVO;
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

import java.util.List;

/**
 * 定时任务批次接口（对接前端 /api/plan/batch/**）
 */
@RestController
@RequestMapping("/api/plan/batch")
@RequiredArgsConstructor
public class PlanBatchController {

    private final PlanBatchService planBatchService;

    /** 批次分页列表 */
    @GetMapping("/list")
    public Result<PageResult<PlanBatchVO>> list(@RequestParam(required = false) Long projectId,
                                               @RequestParam(required = false) String name,
                                               @RequestParam(required = false) Boolean enabled,
                                               @RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size) {
        return Result.success(planBatchService.list(projectId, name, enabled, page, size));
    }

    /** 新建批次 */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody PlanBatchForm form) {
        planBatchService.create(form);
        return Result.ok("批次创建成功");
    }

    /** 编辑批次 */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody PlanBatchForm form) {
        planBatchService.update(form);
        return Result.ok("批次更新成功");
    }

    /** 删除批次 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        planBatchService.delete(id);
        return Result.ok("批次删除成功");
    }

    /** 启/停用批次 */
    @PutMapping("/{id}/enabled")
    public Result<Void> toggleEnabled(@PathVariable Long id, @RequestParam Boolean enabled) {
        planBatchService.toggleEnabled(id, enabled);
        return Result.ok(Boolean.TRUE.equals(enabled) ? "批次已启用" : "批次已停用");
    }

    /** 立即执行批次（手动触发，使用计划默认环境） */
    @PostMapping("/{id}/execute")
    public Result<PlanBatchRunVO> execute(@PathVariable Long id) {
        return Result.success(planBatchService.executeBatch(id, "MANUAL", null));
    }

    /** 轮询：获取某次运行的实时状态 */
    @GetMapping("/run/{runId}")
    public Result<PlanBatchRunVO> getRun(@PathVariable Long runId) {
        return Result.success(planBatchService.getRun(runId));
    }

    /** 某批次的运行历史 */
    @GetMapping("/{id}/runs")
    public Result<List<PlanBatchRunVO>> getRuns(@PathVariable Long id) {
        return Result.success(planBatchService.getRuns(id));
    }

    /** 批次详情（含关联计划） */
    @GetMapping("/{id}")
    public Result<PlanBatchDetailVO> getDetail(@PathVariable Long id) {
        return Result.success(planBatchService.getDetail(id));
    }
}
