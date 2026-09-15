package com.atp.module.base.controller;

import com.atp.common.result.PageResult;
import com.atp.common.result.Result;
import com.atp.module.base.dto.ApiComponentCreateRequest;
import com.atp.module.base.service.ApiComponentService;
import com.atp.module.base.vo.ApiComponentVO;
import com.atp.security.UserPrincipal;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 组合组件接口（可复用公共接口片段）
 */
@RestController
@RequestMapping("/api/component")
@RequiredArgsConstructor
public class ApiComponentController {

    private final ApiComponentService apiComponentService;

    /** 组合组件分页查询（按项目/模块/名称过滤） */
    @GetMapping("/list")
    public Result<PageResult<ApiComponentVO>> list(@RequestParam(required = false) Long projectId,
                                                  @RequestParam(required = false) Long moduleId,
                                                  @RequestParam(required = false) String name,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        return Result.success(apiComponentService.selectComponentPage(page, size, projectId, moduleId, name));
    }

    /** 组件详情（含子步骤） */
    @GetMapping("/{id}")
    public Result<ApiComponentVO> detail(@PathVariable Long id) {
        return Result.success(apiComponentService.getComponentDetail(id));
    }

    /** 新增组件 */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ApiComponentCreateRequest request) {
        apiComponentService.createComponent(request, currentPrincipal().getId());
        return Result.ok("组件新增成功");
    }

    /** 编辑组件 */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ApiComponentCreateRequest request) {
        apiComponentService.updateComponent(request);
        return Result.ok("组件修改成功");
    }

    /** 删除组件 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        apiComponentService.deleteComponent(id);
        return Result.ok("组件删除成功");
    }

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
