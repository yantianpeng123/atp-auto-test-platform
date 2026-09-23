package com.atp.module.ci.controller;

import com.atp.common.result.Result;
import com.atp.module.ci.dto.CiTriggerRequest;
import com.atp.module.ci.service.CiService;
import com.atp.module.ci.vo.CiResultVO;
import com.atp.module.ci.vo.CiRunsPageVO;
import com.atp.module.ci.vo.CiTriggerResultVO;
import com.atp.security.UserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * CI 触发与结果轮询接口（供外部 CI 调用，无需用户 JWT，使用 X-CI-Token 令牌校验）。
 *
 * <p>已在 SecurityConfig 白名单放开：/api/ci/trigger、/api/ci/result/**。
 */
@RestController
@RequestMapping("/api/ci")
@RequiredArgsConstructor
public class CiController {

    private final CiService ciService;

    /** 触发批次执行：CI 令牌 + 项目/批次/环境，立即返回 runId 与轮询地址 */
    @PostMapping("/trigger")
    public Result<CiTriggerResultVO> trigger(@RequestHeader("X-CI-Token") String token,
                                             @RequestBody CiTriggerRequest req) {
        return Result.success(ciService.trigger(req, token));
    }

    /** 轮询某次运行结果：供 CI 判断 build 红绿 */
    @GetMapping("/result/{runId}")
    public Result<CiResultVO> result(@RequestHeader("X-CI-Token") String token,
                                     @PathVariable Long runId) {
        return Result.success(ciService.getResult(runId, token));
    }

    /** 按项目分页列出 CI 触发产生的运行记录（平台内运行历史查看，需登录）。 */
    @GetMapping("/runs")
    public Result<CiRunsPageVO> runs(@RequestParam Long projectId,
                                    @RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "10") long size) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return Result.success(ciService.getRuns(projectId, principal, page, size));
    }

    /** 生成 JUnit 格式 XML 报告：外部 CI 携带 X-CI-Token 调用；平台内查看则走登录态（JWT）。 */
    @GetMapping(value = "/report/{runId}.xml")
    public void report(@RequestHeader(value = "X-CI-Token", required = false) String token,
                      @PathVariable Long runId,
                      HttpServletResponse response) throws IOException {
        String xml;
        if (token != null && !token.isBlank()) {
            xml = ciService.buildReportXml(runId, token);
        } else {
            UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            xml = ciService.buildReportXml(runId, principal);
        }
        response.setContentType("application/xml;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(xml);
    }
}
