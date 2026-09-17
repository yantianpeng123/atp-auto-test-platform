package com.atp.module.base.service.impl;

import cn.hutool.json.JSONUtil;
import com.atp.common.exception.BizException;
import com.atp.common.result.PageResult;
import com.atp.common.result.ResultCode;
import com.atp.module.base.dto.DataGeneratorSaveRequest;
import com.atp.module.base.entity.DataGenerator;
import com.atp.module.base.generator.GeneratorEngine;
import com.atp.module.base.mapper.DataGeneratorMapper;
import com.atp.module.base.service.DataGeneratorService;
import com.atp.module.base.vo.DataGeneratorVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 数据生成器服务实现
 *
 * <p>{@code params} 在库里是 JSON 列、实体里是 JSON 文本，结构化/反结构化都在本类完成，
 * 对外（VO / DTO）一律是 {@code Map}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataGeneratorServiceImpl extends ServiceImpl<DataGeneratorMapper, DataGenerator>
        implements DataGeneratorService {

    @Override
    public PageResult<DataGeneratorVO> selectGeneratorPage(long page, long size, Long projectId,
                                                           String name, String type) {
        Page<DataGenerator> pageParam = new Page<>(page, size);
        QueryWrapper<DataGenerator> qw = new QueryWrapper<>();
        if (projectId != null) {
            qw.eq("project_id", projectId);
        }
        if (StringUtils.hasText(name)) {
            qw.like("name", name);
        }
        if (StringUtils.hasText(type)) {
            qw.eq("type", type.trim().toUpperCase());
        }
        qw.orderByDesc("update_time");

        IPage<DataGenerator> result = baseMapper.selectPage(pageParam, qw);
        return PageResult.<DataGeneratorVO>builder()
                .records(result.getRecords().stream().map(this::toVO).toList())
                .total(result.getTotal())
                .page(result.getCurrent())
                .size(result.getSize())
                .build();
    }

    @Override
    public DataGeneratorVO getGenerator(Long id) {
        return toVO(requireGenerator(id));
    }

    @Override
    public DataGeneratorVO createGenerator(DataGeneratorSaveRequest request, Long userId) {
        String type = normalizeType(request.getType());
        checkNameDuplicate(request.getProjectId(), request.getName(), null);

        DataGenerator entity = new DataGenerator();
        entity.setProjectId(request.getProjectId());
        entity.setName(request.getName().trim());
        entity.setType(type);
        entity.setParams(toParamsJson(request.getParams()));
        entity.setDescription(request.getDescription());
        entity.setCreateBy(userId == null ? null : String.valueOf(userId));
        baseMapper.insert(entity);

        return toVO(entity);
    }

    @Override
    public DataGeneratorVO updateGenerator(DataGeneratorSaveRequest request) {
        DataGenerator entity = requireGenerator(request.getId());
        String type = normalizeType(request.getType());
        checkNameDuplicate(entity.getProjectId(), request.getName(), entity.getId());

        entity.setName(request.getName().trim());
        entity.setType(type);
        entity.setParams(toParamsJson(request.getParams()));
        entity.setDescription(request.getDescription());
        baseMapper.updateById(entity);

        return toVO(entity);
    }

    @Override
    public void deleteGenerator(Long id) {
        requireGenerator(id);
        baseMapper.deleteById(id);
    }

    // ==================== 内部辅助 ====================

    /** 校验类型并统一为大写；白名单与 GeneratorEngine 保持一致 */
    private String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            throw new BizException(ResultCode.BAD_REQUEST, "生成器类型不能为空");
        }
        String normalized = type.trim().toUpperCase();
        if (!GeneratorEngine.supportedTypes().contains(normalized)) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "不支持的生成器类型：" + type + "，可用类型：" + String.join("、", GeneratorEngine.supportedTypes()));
        }
        return normalized;
    }

    private DataGenerator requireGenerator(Long id) {
        if (id == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "生成器 ID 不能为空");
        }
        DataGenerator entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.NOT_FOUND, "生成器不存在或已被删除");
        }
        return entity;
    }

    /** 同一项目下名称不可重复（编辑时排除自身） */
    private void checkNameDuplicate(Long projectId, String name, Long excludeId) {
        QueryWrapper<DataGenerator> qw = new QueryWrapper<>();
        qw.eq("project_id", projectId).eq("name", name == null ? "" : name.trim());
        if (excludeId != null) {
            qw.ne("id", excludeId);
        }
        if (baseMapper.selectCount(qw) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "当前项目下已存在同名生成器：" + name);
        }
    }

    private String toParamsJson(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        return JSONUtil.toJsonStr(params);
    }

    private Map<String, Object> parseParams(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSONUtil.parseObj(json);
        } catch (Exception e) {
            log.warn("生成器 params 不是合法 JSON，按空处理：{}", json);
            return null;
        }
    }

    private DataGeneratorVO toVO(DataGenerator entity) {
        if (entity == null) {
            return null;
        }
        return DataGeneratorVO.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .name(entity.getName())
                .type(entity.getType())
                .params(parseParams(entity.getParams()))
                .description(entity.getDescription())
                .createTime(entity.getCreateTime())
                .build();
    }
}
