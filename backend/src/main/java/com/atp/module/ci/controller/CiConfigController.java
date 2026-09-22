package com.atp.module.ci.controller;

import com.atp.common.result.Result;
import com.atp.module.ci.dto.CiConfigSaveRequest;
import com.atp.module.ci.service.CiService;
import com.atp.module.ci.vo.CiConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CI 配置管理接口（需用户登录 JWT，用于创建/查看令牌、配置默认批次与环境）。
 *
 * <p>路径 /api/ci/config/** 不在白名单内，因此受 SecurityConfig 的 JWT 鉴权保护，
 * 与对外免登录的 /api/ci/trigger、/api/ci/result/** 区分开。
 */
@RestController
@RequestMapping("/api/ci/config")
@RequiredArgsConstructor
public class CiConfigController {

    private final CiService ciService;

    /** 新增/更新 CI 配置（新建时返回一次性明文令牌） */
    @PostMapping
    public Result<CiConfigVO> upsert(@RequestBody CiConfigSaveRequest req) {
        return Result.success(ciService.upsertConfig(req));
    }

    /** 按项目查询配置（不含明文令牌） */
    @GetMapping
    public Result<CiConfigVO> get(@RequestParam Long projectId) {
        return Result.success(ciService.getConfig(projectId));
    }

    /** 重新生成令牌（旧令牌立即失效，返回新明文令牌） */
    @PostMapping("/{projectId}/token")
    public Result<CiConfigVO> regenerate(@PathVariable Long projectId) {
        return Result.success(ciService.regenerateToken(projectId));
    }
}
