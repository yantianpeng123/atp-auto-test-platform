package com.atp.module.base.controller;

import com.atp.common.result.PageResult;
import com.atp.common.result.Result;
import com.atp.module.base.dto.DataGeneratorSaveRequest;
import com.atp.module.base.dto.GeneratorPreviewRequest;
import com.atp.module.base.generator.GeneratorEngine;
import com.atp.module.base.generator.GeneratorFunc;
import com.atp.module.base.service.DataGeneratorService;
import com.atp.module.base.vo.DataGeneratorVO;
import com.atp.module.base.vo.GeneratorFunctionVO;
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

import java.util.List;
import java.util.Map;

/**
 * 数据生成器接口。
 *
 * <p>阶段 1 提供表达式引擎与试生成（{@code /functions}、{@code /preview}）；
 * 阶段 2 补齐生成器本身的持久化 CRUD（{@code /list}、增改删、详情）。
 * 生成变量步骤（{@code step_type=3}）的接入属于阶段 3。
 */
@RestController
@RequestMapping("/api/base/generator")
@RequiredArgsConstructor
public class GeneratorController {

    private final DataGeneratorService dataGeneratorService;

    /** 可用生成函数清单（前端帮助/语法提示） */
    @GetMapping("/functions")
    public Result<List<GeneratorFunctionVO>> functions() {
        List<GeneratorFunctionVO> list = GeneratorEngine.functions().stream()
                .map(f -> GeneratorFunctionVO.builder()
                        .name(f.funcName())
                        .args(f.argsDoc())
                        .desc(f.desc())
                        .example(f.example())
                        .build())
                .toList();
        return Result.success(list);
    }

    /** 试生成：传 type + params（CUSTOM 时 params.template），返回 { result } */
    @PostMapping("/preview")
    public Result<Map<String, String>> preview(@Valid @RequestBody GeneratorPreviewRequest request) {
        String value = GeneratorEngine.generate(request.getType(), request.getParams());
        return Result.success(Map.of("result", value));
    }

    /** 生成器分页查询（按项目 / 名称 / 类型过滤） */
    @GetMapping("/list")
    public Result<PageResult<DataGeneratorVO>> list(@RequestParam(required = false) Long projectId,
                                                    @RequestParam(required = false) String name,
                                                    @RequestParam(required = false) String type,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size) {
        return Result.success(dataGeneratorService.selectGeneratorPage(page, size, projectId, name, type));
    }

    /** 生成器详情 */
    @GetMapping("/{id}")
    public Result<DataGeneratorVO> detail(@PathVariable Long id) {
        return Result.success(dataGeneratorService.getGenerator(id));
    }

    /** 新增生成器，返回保存后的完整信息 */
    @PostMapping
    public Result<DataGeneratorVO> create(@Valid @RequestBody DataGeneratorSaveRequest request) {
        return Result.success(dataGeneratorService.createGenerator(request, currentPrincipal().getId()));
    }

    /** 编辑生成器，返回保存后的完整信息 */
    @PutMapping
    public Result<DataGeneratorVO> update(@Valid @RequestBody DataGeneratorSaveRequest request) {
        return Result.success(dataGeneratorService.updateGenerator(request));
    }

    /** 删除生成器（逻辑删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dataGeneratorService.deleteGenerator(id);
        return Result.ok("生成器删除成功");
    }

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
