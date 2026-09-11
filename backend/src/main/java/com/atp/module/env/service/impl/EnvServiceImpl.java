package com.atp.module.env.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.env.dto.EnvCreateRequest;
import com.atp.module.env.dto.EnvUpdateRequest;
import com.atp.module.env.entity.TestEnv;
import com.atp.module.env.mapper.TestEnvMapper;
import com.atp.module.env.service.EnvService;
import com.atp.module.env.vo.EnvVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 测试环境服务实现
 */
@Service
@RequiredArgsConstructor
public class EnvServiceImpl extends ServiceImpl<TestEnvMapper, TestEnv> implements EnvService {

    private final ObjectMapper objectMapper;

    @Override
    public IPage<EnvVO> selectEnvPage(long page, long size, Long projectId, String name) {
        LambdaQueryWrapper<TestEnv> wrapper = new LambdaQueryWrapper<TestEnv>()
                .eq(projectId != null, TestEnv::getProjectId, projectId)
                .like(StringUtils.hasText(name), TestEnv::getName, name)
                .orderByDesc(TestEnv::getUpdateTime);

        IPage<TestEnv> entityPage = baseMapper.selectPage(new Page<>(page, size), wrapper);

        Page<EnvVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<EnvVO> records = entityPage.getRecords().stream().map(this::toVO).toList();
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createEnv(EnvCreateRequest request) {
        checkNameUnique(request.getProjectId(), request.getName(), null);

        TestEnv env = new TestEnv();
        env.setProjectId(request.getProjectId());
        env.setName(request.getName().trim());
        env.setBaseUrl(trimToNull(request.getBaseUrl()));
        env.setHeaders(normalizeJson(request.getHeaders(), "全局请求头"));
        env.setDbConfig(normalizeJson(request.getDbConfig(), "数据库配置"));
        save(env);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnv(EnvUpdateRequest request) {
        TestEnv env = getById(request.getId());
        if (env == null) {
            throw new BizException(ResultCode.ENV_NOT_FOUND);
        }
        checkNameUnique(env.getProjectId(), request.getName(), request.getId());

        env.setName(request.getName().trim());
        env.setBaseUrl(trimToNull(request.getBaseUrl()));
        env.setHeaders(normalizeJson(request.getHeaders(), "全局请求头"));
        env.setDbConfig(normalizeJson(request.getDbConfig(), "数据库配置"));
        updateById(env);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEnv(Long id) {
        TestEnv env = getById(id);
        if (env == null) {
            throw new BizException(ResultCode.ENV_NOT_FOUND);
        }
        removeById(id);
    }

    /** 同一项目下环境名称不可重复 */
    private void checkNameUnique(Long projectId, String name, Long excludeId) {
        long count = lambdaQuery()
                .eq(TestEnv::getProjectId, projectId)
                .eq(TestEnv::getName, name.trim())
                .ne(excludeId != null, TestEnv::getId, excludeId)
                .count();
        if (count > 0) {
            throw new BizException(ResultCode.ENV_NAME_EXISTS);
        }
    }

    /**
     * JSON 字段落库前校验，非法 JSON 直接拒绝，避免执行引擎读取时才炸
     */
    private String normalizeJson(String json, String fieldName) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            objectMapper.readTree(json);
        } catch (Exception e) {
            throw new BizException(ResultCode.BAD_REQUEST, fieldName + "不是合法的 JSON");
        }
        return json.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private EnvVO toVO(TestEnv env) {
        return EnvVO.builder()
                .id(env.getId())
                .projectId(env.getProjectId())
                .name(env.getName())
                .baseUrl(env.getBaseUrl())
                .headers(env.getHeaders())
                .dbConfig(env.getDbConfig())
                .createTime(env.getCreateTime())
                .updateTime(env.getUpdateTime())
                .build();
    }
}
