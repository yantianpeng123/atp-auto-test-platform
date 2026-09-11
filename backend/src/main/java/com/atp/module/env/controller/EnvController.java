package com.atp.module.env.controller;

import com.atp.common.result.PageResult;
import com.atp.common.result.Result;
import com.atp.module.env.dto.EnvCreateRequest;
import com.atp.module.env.dto.EnvUpdateRequest;
import com.atp.module.env.service.EnvService;
import com.atp.module.env.vo.EnvVO;
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
 * 测试环境接口
 */
@RestController
@RequestMapping("/api/env")
@RequiredArgsConstructor
public class EnvController {

    private final EnvService envService;

    /** 环境分页查询 */
    @GetMapping("/list")
    public Result<PageResult<EnvVO>> list(@RequestParam(required = false) Long projectId,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size) {
        IPage<EnvVO> result = envService.selectEnvPage(page, size, projectId, name);
        PageResult<EnvVO> pageResult = PageResult.<EnvVO>builder()
                .records(result.getRecords())
                .total(result.getTotal())
                .page(result.getCurrent())
                .size(result.getSize())
                .build();
        return Result.success(pageResult);
    }

    /** 新增环境 */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody EnvCreateRequest request) {
        envService.createEnv(request);
        return Result.ok("环境新增成功");
    }

    /** 编辑环境 */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody EnvUpdateRequest request) {
        envService.updateEnv(request);
        return Result.ok("环境修改成功");
    }

    /** 删除环境 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        envService.deleteEnv(id);
        return Result.ok("环境删除成功");
    }
}
