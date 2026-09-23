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
import com.atp.module.plan.vo.PlanBatchRunVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

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
        if (token == null || token.isBlank()) {
            throw new BizException(ResultCode.CI_TOKEN_INVALID, "缺少 CI 令牌");
        }
        PlanBatchRunVO vo = planBatchService.getRun(runId);

        PlanBatch batch = planBatchMapper.selectById(vo.getBatchId());
        if (batch == null) {
            throw new BizException(ResultCode.BATCH_NOT_FOUND);
        }
        CiConfig config = ciConfigMapper.selectOne(
                new QueryWrapper<CiConfig>().eq("project_id", batch.getProjectId()).eq("deleted", 0));
        if (config == null || !passwordEncoder.matches(token, config.getTokenHash())) {
            throw new BizException(ResultCode.CI_TOKEN_INVALID, "CI 令牌无效");
        }

        List<Object> items = vo.getItems() != null ? new ArrayList<>(vo.getItems()) : null;
        return CiResultVO.builder()
                .runId(vo.getId())
                .batchId(vo.getBatchId())
                .status(vo.getStatus())
                .total(vo.getTotal())
                .passed(vo.getPassed())
                .failed(vo.getFailed())
                .running(vo.getRunning())
                .queued(vo.getQueued())
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
