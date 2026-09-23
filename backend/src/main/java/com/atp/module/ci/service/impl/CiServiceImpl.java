package com.atp.module.ci.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.ci.dto.CiConfigSaveRequest;
import com.atp.module.ci.dto.CiTriggerRequest;
import com.atp.module.ci.entity.CiConfig;
import com.atp.module.ci.mapper.CiConfigMapper;
import com.atp.module.ci.service.CiService;
import com.atp.module.ci.vo.CiConfigVO;
import com.atp.module.ci.vo.CiResultVO;
import com.atp.module.ci.vo.CiTriggerResultVO;
import com.atp.module.plan.entity.PlanBatch;
import com.atp.module.plan.mapper.PlanBatchMapper;
import com.atp.module.plan.service.PlanBatchService;
import com.atp.module.project.service.ProjectMemberService;
import com.atp.module.execute.entity.ExecutionAssertion;
import com.atp.module.execute.entity.ExecutionDetail;
import com.atp.module.execute.mapper.ExecutionAssertionMapper;
import com.atp.module.execute.mapper.ExecutionDetailMapper;
import com.atp.module.plan.entity.PlanBatchRun;
import com.atp.module.plan.entity.PlanBatchRunItem;
import com.atp.module.plan.mapper.PlanBatchRunItemMapper;
import com.atp.module.plan.mapper.PlanBatchRunMapper;
import com.atp.module.ci.vo.CiRunsPageVO;
import com.atp.module.ci.vo.CiRunStatusCount;
import com.atp.module.ci.vo.CiRunItem;
import com.atp.module.plan.vo.PlanBatchRunVO;
import com.atp.module.testcase.entity.TestCase;
import com.atp.module.testcase.mapper.TestCaseMapper;
import com.atp.security.UserPrincipal;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * CI 集成服务实现。
 *
 * <p>令牌采用 BCrypt 哈希存储，触发与轮询均需在 {@code X-CI-Token} 头携带明文令牌，
 * 服务端用 {@link PasswordEncoder#matches} 校验，避免明文落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CiServiceImpl implements CiService {

    private final CiConfigMapper ciConfigMapper;
    private final PlanBatchMapper planBatchMapper;
    private final PlanBatchService planBatchService;
    private final PasswordEncoder passwordEncoder;
    private final PlanBatchRunMapper planBatchRunMapper;
    private final PlanBatchRunItemMapper planBatchRunItemMapper;
    private final ProjectMemberService projectMemberService;
    private final ExecutionDetailMapper executionDetailMapper;
    private final ExecutionAssertionMapper executionAssertionMapper;
    private final TestCaseMapper testCaseMapper;

    @Override
    public CiTriggerResultVO trigger(CiTriggerRequest req, String token) {
        if (req.getProjectId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "projectId 不能为空");
        }
        if (token == null || token.isBlank()) {
            throw new BizException(ResultCode.CI_TOKEN_INVALID, "缺少 CI 令牌");
        }
        CiConfig config = ciConfigMapper.selectOne(
                new QueryWrapper<CiConfig>().eq("project_id", req.getProjectId()).eq("deleted", 0));
        if (config == null || config.getEnabled() == null || config.getEnabled() != 1) {
            throw new BizException(ResultCode.CI_CONFIG_NOT_FOUND, "CI 配置不存在或未启用");
        }
        if (!passwordEncoder.matches(token, config.getTokenHash())) {
            throw new BizException(ResultCode.CI_TOKEN_INVALID, "CI 令牌无效");
        }

        Long batchId = req.getBatchId() != null ? req.getBatchId() : config.getDefaultBatchId();
        if (batchId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "未指定批次且配置未设置默认批次");
        }
        Long envId = req.getEnvId() != null ? req.getEnvId() : config.getDefaultEnvId();

        // 后台异步执行，立即返回运行实例ID
        Long runId = planBatchService.executeBatchAsync(batchId, "CI", envId);

        return CiTriggerResultVO.builder()
                .runId(runId)
                .statusUrl("/api/ci/result/" + runId)
                .status("RUNNING")
                .build();
    }

    @Override
    public CiResultVO getResult(Long runId, String token) {
        checkToken(runId, token);
        PlanBatchRunVO vo = planBatchService.getRun(runId);
        PlanBatch batch = planBatchMapper.selectById(vo.getBatchId());
        String batchName = batch != null ? batch.getName() : null;

        List<Object> items = vo.getItems() != null ? new ArrayList<>(vo.getItems()) : null;
        return CiResultVO.builder()
                .runId(vo.getId())
                .batchId(vo.getBatchId())
                .batchName(batchName)
                .triggerType(vo.getTriggerType())
                .status(vo.getStatus())
                .total(vo.getTotal())
                .passed(vo.getPassed())
                .failed(vo.getFailed())
                .running(vo.getRunning())
                .queued(vo.getQueued())
                .startTime(vo.getStartTime())
                .endTime(vo.getEndTime())
                .durationMs(vo.getDurationMs())
                .summaryUrl("/plan/batch/" + vo.getBatchId())
                .items(items)
                .build();
    }

    @Override
    public CiConfigVO upsertConfig(CiConfigSaveRequest req) {
        if (req.getProjectId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "projectId 不能为空");
        }
        CiConfig config = ciConfigMapper.selectOne(
                new QueryWrapper<CiConfig>().eq("project_id", req.getProjectId()).eq("deleted", 0));
        boolean created = false;
        String plainToken = null;

        if (config == null) {
            config = new CiConfig();
            config.setProjectId(req.getProjectId());
            plainToken = generateToken();
            config.setTokenHash(passwordEncoder.encode(plainToken));
            config.setEnabled(1);
            created = true;
        }
        if (req.getDefaultEnvId() != null) {
            config.setDefaultEnvId(req.getDefaultEnvId());
        }
        if (req.getDefaultBatchId() != null) {
            config.setDefaultBatchId(req.getDefaultBatchId());
        }
        if (req.getCallbackUrl() != null) {
            config.setCallbackUrl(req.getCallbackUrl());
        }
        if (req.getEnabled() != null) {
            config.setEnabled(req.getEnabled() ? 1 : 0);
        }

        if (created) {
            ciConfigMapper.insert(config);
        } else {
            ciConfigMapper.updateById(config);
        }
        return toVO(config, plainToken);
    }

    @Override
    public CiConfigVO getConfig(Long projectId) {
        CiConfig config = ciConfigMapper.selectOne(
                new QueryWrapper<CiConfig>().eq("project_id", projectId).eq("deleted", 0));
        // 未配置时返回 null（而非抛错），前端据此呈现「尚未启用」的初始态，避免首屏错误提示
        return config == null ? null : toVO(config, null);
    }

    @Override
    public CiConfigVO regenerateToken(Long projectId) {
        CiConfig config = ciConfigMapper.selectOne(
                new QueryWrapper<CiConfig>().eq("project_id", projectId).eq("deleted", 0));
        if (config == null) {
            throw new BizException(ResultCode.CI_CONFIG_NOT_FOUND);
        }
        String plainToken = generateToken();
        config.setTokenHash(passwordEncoder.encode(plainToken));
        ciConfigMapper.updateById(config);
        return toVO(config, plainToken);
    }

    /** 按项目列出 CI 运行记录（平台内查看，需登录且为项目成员/管理员） */
    @Override
    public CiRunsPageVO getRuns(Long projectId, UserPrincipal principal, long page, long size) {
        // 项目成员或管理员可见；全局管理员放行，非管理员需要是项目成员
        boolean isAdmin = "ADMIN".equals(principal.getRole());
        if (!isAdmin && projectMemberService.getMyRole(projectId, principal.getId(), false) == null) {
            throw new BizException(ResultCode.FORBIDDEN, "无权访问该项目");
        }
        Page<CiRunItem> p = new Page<>(page, size);
        IPage<CiRunItem> ip = planBatchRunMapper.selectCiRuns(p, projectId);

        // 按状态聚合全部数据的计数（不受分页影响，供统计卡使用）
        List<CiRunStatusCount> counts = planBatchRunMapper.countCiRunsByStatus(projectId);
        long success = 0, partialFailed = 0, failed = 0, running = 0;
        for (CiRunStatusCount c : counts) {
            switch (c.getStatus()) {
                case "SUCCESS" -> success += c.getCnt();
                case "PARTIAL_FAILED" -> partialFailed += c.getCnt();
                case "FAILED" -> failed += c.getCnt();
                case "RUNNING" -> running += c.getCnt();
                default -> { /* 未知状态忽略 */ }
            }
        }
        long totalCount = ip.getTotal();
        CiRunsPageVO.CiRunSummary summary = CiRunsPageVO.CiRunSummary.builder()
                .total(totalCount)
                .success(success)
                .partialFailed(partialFailed)
                .failed(failed)
                .running(running)
                .build();

        return CiRunsPageVO.builder()
                .records(ip.getRecords())
                .total(totalCount)
                .page(ip.getCurrent())
                .size(ip.getSize())
                .summary(summary)
                .build();
    }

    /** 校验 run 存在且调用方持有合法 CI 令牌（按 run → batch → project → CiConfig 链路核对） */
    private void checkToken(Long runId, String token) {
        if (token == null || token.isBlank()) {
            throw new BizException(ResultCode.CI_TOKEN_INVALID, "缺少 CI 令牌");
        }
        PlanBatchRun run = planBatchRunMapper.selectById(runId);
        if (run == null) {
            throw new BizException(ResultCode.NOT_FOUND, "运行实例不存在");
        }
        PlanBatch batch = planBatchMapper.selectById(run.getBatchId());
        if (batch == null) {
            throw new BizException(ResultCode.BATCH_NOT_FOUND);
        }
        CiConfig config = ciConfigMapper.selectOne(
                new QueryWrapper<CiConfig>().eq("project_id", batch.getProjectId()).eq("deleted", 0));
        if (config == null || !passwordEncoder.matches(token, config.getTokenHash())) {
            throw new BizException(ResultCode.CI_TOKEN_INVALID, "CI 令牌无效");
        }
    }

    /**
     * 生成 JUnit 格式 XML 报告：
     * 外层 testsuite 对应「用例」（name/状态/耗时/错误），内层 testcase 对应「步骤」，
     * 步骤的 failure 节点内展开每条断言明细（类型/操作符/期望/实际/原因）。
     */
    @Override
    public String buildReportXml(Long runId, String token) {
        checkToken(runId, token);
        return buildReportXmlInternal(runId);
    }

    @Override
    public String buildReportXml(Long runId, UserPrincipal principal) {
        // 平台内查看：run → batch → project 校验当前用户是否为成员/管理员
        PlanBatchRun run = planBatchRunMapper.selectById(runId);
        if (run == null) {
            throw new BizException(ResultCode.NOT_FOUND, "运行实例不存在");
        }
        PlanBatch batch = planBatchMapper.selectById(run.getBatchId());
        if (batch == null) {
            throw new BizException(ResultCode.BATCH_NOT_FOUND);
        }
        // 全局管理员放行，非管理员需要是项目成员
        boolean isAdmin = "ADMIN".equals(principal.getRole());
        if (!isAdmin && projectMemberService.getMyRole(batch.getProjectId(), principal.getId(), false) == null) {
            throw new BizException(ResultCode.FORBIDDEN, "无权访问该运行记录");
        }
        return buildReportXmlInternal(runId);
    }

    /** 实际生成 JUnit XML（调用方已完成鉴权） */
    private String buildReportXmlInternal(Long runId) {
        PlanBatchRun run = planBatchRunMapper.selectById(runId);
        PlanBatch batch = planBatchMapper.selectById(run.getBatchId());
        String rootName = batch != null && batch.getName() != null ? batch.getName() : ("run-" + runId);

        List<PlanBatchRunItem> items = planBatchRunItemMapper.selectList(
                new QueryWrapper<PlanBatchRunItem>().eq("run_id", runId));
        List<Long> execIds = items.stream()
                .map(PlanBatchRunItem::getExecutionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (execIds.isEmpty()) {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<testsuites name=\"" + esc("批次: " + rootName + " (run " + runId + ")")
                    + "\" tests=\"0\" failures=\"0\" time=\"0.000\"></testsuites>\n";
        }

        List<ExecutionDetail> details = executionDetailMapper.selectList(
                new QueryWrapper<ExecutionDetail>()
                        .in("execution_id", execIds)
                        .orderByAsc("case_id").orderByAsc("round_index").orderByAsc("step_index"));
        List<ExecutionAssertion> assertions = executionAssertionMapper.selectList(
                new QueryWrapper<ExecutionAssertion>().in("execution_id", execIds));
        Map<Long, List<ExecutionAssertion>> assertByDetail = assertions.stream()
                .collect(Collectors.groupingBy(ExecutionAssertion::getDetailId));

        Map<Long, List<ExecutionDetail>> byCase = details.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getCaseId() == null ? -1L : d.getCaseId(),
                        LinkedHashMap::new, Collectors.toList()));

        Map<Long, String> caseNameCache = new java.util.HashMap<>();

        StringBuilder caseSuites = new StringBuilder();
        int totalSteps = 0;
        int totalFailures = 0;
        double totalTimeSec = 0.0;

        for (Map.Entry<Long, List<ExecutionDetail>> en : byCase.entrySet()) {
            Long caseId = en.getKey();
            List<ExecutionDetail> steps = en.getValue();

            String caseName = caseNameCache.computeIfAbsent(caseId, cid -> {
                if (cid == null || cid == -1L) {
                    return "未归类步骤";
                }
                TestCase tc = testCaseMapper.selectById(cid);
                return tc != null && tc.getName() != null ? tc.getName() : ("用例#" + cid);
            });

            StringBuilder stepCases = new StringBuilder();
            int caseSteps = 0;
            int caseFailures = 0;
            double caseTimeSec = 0.0;

            for (ExecutionDetail d : steps) {
                caseSteps++;
                totalSteps++;
                double stepSec = (d.getDurationMs() == null ? 0 : d.getDurationMs()) / 1000.0;
                caseTimeSec += stepSec;
                totalTimeSec += stepSec;

                String stepName = d.getStepName() == null ? ("步骤#" + d.getStepId()) : d.getStepName();
                if (d.getRoundIndex() != null && d.getRoundIndex() > 1) {
                    stepName += " (第" + d.getRoundIndex() + "轮)";
                }

                List<ExecutionAssertion> ads = assertByDetail.get(d.getId());
                boolean hasFailedAssert = ads != null && ads.stream()
                        .anyMatch(a -> a.getPassed() == null || a.getPassed() == 0);
                boolean stepFailed = "FAILED".equals(d.getStatus())
                        || "ERROR".equals(d.getStatus()) || hasFailedAssert;

                stepCases.append("    <testcase name=\"").append(esc(stepName)).append("\" time=\"")
                        .append(fmt(stepSec)).append("\">");
                if (stepFailed) {
                    caseFailures++;
                    totalFailures++;
                    stepCases.append("<failure message=\"").append(esc(buildFailureMessage(d, ads)))
                            .append("\" type=\"AssertionFailed\">")
                            .append(esc(buildFailureDetail(d, ads))).append("</failure>");
                }
                stepCases.append("</testcase>\n");
            }

            caseSuites.append("  <testsuite name=\"").append(esc(caseName)).append("\" tests=\"")
                    .append(caseSteps).append("\" failures=\"").append(caseFailures)
                    .append("\" time=\"").append(fmt(caseTimeSec)).append("\">\n")
                    .append(stepCases)
                    .append("  </testsuite>\n");
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<testsuites name=\"" + esc("批次: " + rootName + " (run " + runId + ")")
                + "\" tests=\"" + totalSteps + "\" failures=\"" + totalFailures
                + "\" time=\"" + fmt(totalTimeSec) + "\">\n"
                + caseSuites
                + "</testsuites>\n";
    }

    private String buildFailureMessage(ExecutionDetail d, List<ExecutionAssertion> ads) {
        if (d.getErrorMsg() != null && !d.getErrorMsg().isBlank()) {
            String m = d.getErrorMsg();
            return m.length() > 200 ? m.substring(0, 200) : m;
        }
        if (ads != null) {
            for (ExecutionAssertion a : ads) {
                if (a.getPassed() == null || a.getPassed() == 0) {
                    return a.getMessage() != null ? a.getMessage()
                            : ("断言失败: " + a.getType() + " " + a.getOperator());
                }
            }
        }
        return "步骤状态: " + (d.getStatus() == null ? "UNKNOWN" : d.getStatus());
    }

    private String buildFailureDetail(ExecutionDetail d, List<ExecutionAssertion> ads) {
        StringBuilder sb = new StringBuilder();
        if (ads != null) {
            for (ExecutionAssertion a : ads) {
                if (a.getPassed() != null && a.getPassed() == 1) {
                    continue;
                }
                sb.append("[").append(a.getType() == null ? "" : a.getType());
                if (a.getPath() != null && !a.getPath().isBlank()) {
                    sb.append(" ").append(a.getPath());
                }
                sb.append("] ").append(a.getOperator() == null ? "" : a.getOperator())
                        .append(" expected=").append(a.getExpected() == null ? "" : a.getExpected())
                        .append(" actual=").append(a.getActual() == null ? "" : a.getActual());
                if (a.getMessage() != null && !a.getMessage().isBlank()) {
                    sb.append(" (").append(a.getMessage()).append(")");
                }
                sb.append("\n");
            }
        }
        if (sb.length() == 0) {
            if (d.getErrorMsg() != null && !d.getErrorMsg().isBlank()) {
                sb.append(d.getErrorMsg());
            } else {
                sb.append("步骤状态: ").append(d.getStatus() == null ? "UNKNOWN" : d.getStatus());
            }
        }
        return sb.toString().trim();
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String fmt(double v) {
        return String.format("%.3f", v);
    }

    private CiConfigVO toVO(CiConfig c, String plainToken) {
        return CiConfigVO.builder()
                .id(c.getId())
                .projectId(c.getProjectId())
                .defaultEnvId(c.getDefaultEnvId())
                .defaultBatchId(c.getDefaultBatchId())
                .callbackUrl(c.getCallbackUrl())
                .enabled(c.getEnabled())
                .token(plainToken)
                .tokenHint(plainToken != null && plainToken.length() >= 4
                        ? plainToken.substring(plainToken.length() - 4) : null)
                .createTime(c.getCreateTime())
                .updateTime(c.getUpdateTime())
                .build();
    }

    /** 生成形如 ci_<64位十六进制> 的随机令牌 */
    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder("ci_");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
