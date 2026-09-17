package com.atp.module.base.controller;

import com.atp.common.result.Result;
import com.atp.module.base.dto.GeneratorPreviewRequest;
import com.atp.module.base.generator.GeneratorEngine;
import com.atp.module.base.generator.GeneratorFunc;
import com.atp.module.base.vo.GeneratorFunctionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据生成器接口。
 *
 * <p>阶段 1 已提供：函数清单 + 试生成。生成器的持久化（建表 CRUD）见设计文档 §3.3，属于阶段 2。
 */
@RestController
@RequestMapping("/api/base/generator")
@RequiredArgsConstructor
public class GeneratorController {

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
}
