package com.atp.module.base.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.base.dto.ApiComponentCreateRequest;
import com.atp.module.base.entity.ApiComponent;
import com.atp.module.base.entity.ApiComponentStep;
import com.atp.module.base.generator.GeneratorEngine;
import com.atp.module.base.mapper.ApiComponentMapper;
import com.atp.module.base.mapper.DataGeneratorMapper;
import com.atp.module.base.mapper.ApiComponentStepMapper;
import com.atp.module.base.service.ApiComponentService;
import com.atp.module.base.vo.ApiComponentVO;
import com.atp.module.testcase.vo.CaseStepVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 组合组件服务实现
 */
@Service
@RequiredArgsConstructor
public class ApiComponentServiceImpl extends ServiceImpl<ApiComponentMapper, ApiComponent>
        implements ApiComponentService {

    private final ApiComponentStepMapper componentStepMapper;
    private final DataGeneratorMapper dataGeneratorMapper;

    @Override
    public com.atp.common.result.PageResult<ApiComponentVO> selectComponentPage(long page, long size,
                                                                              Long projectId, Long moduleId, String name) {
        Page<ApiComponent> pageParam = new Page<>(page, size);
        QueryWrapper<ApiComponent> qw = new QueryWrapper<>();
        if (projectId != null) {
            qw.eq("project_id", projectId);
        }
        if (moduleId != null) {
            qw.eq("module_id", moduleId);
        }
        if (StringUtils.hasText(name)) {
            qw.like("name", name);
        }
        qw.orderByDesc("update_time");
        IPage<ApiComponent> result = baseMapper.selectPage(pageParam, qw);

        List<ApiComponentVO> vos = result.getRecords().stream().map(this::toVO).toList();
        return com.atp.common.result.PageResult.<ApiComponentVO>builder()
                .records(vos)
                .total(result.getTotal())
                .page(result.getCurrent())
                .size(result.getSize())
                .build();
    }

    @Override
    public ApiComponentVO getComponentDetail(Long id) {
        ApiComponent component = getById(id);
        if (component == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "组件不存在");
        }
        ApiComponentVO vo = toVO(component);
        List<CaseStepVO> steps = componentStepMapper.selectComponentSteps(id);
        vo.setSteps(steps);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createComponent(ApiComponentCreateRequest request, Long userId) {
        ApiComponent component = new ApiComponent();
        component.setProjectId(request.getProjectId());
        component.setModuleId(request.getModuleId());
        component.setName(request.getName().trim());
        component.setDescription(trimToNull(request.getDescription()));
        component.setCreateBy(userId);
        save(component);
        saveSteps(component.getId(), request.getSteps());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateComponent(ApiComponentCreateRequest request) {
        if (request.getId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "组件ID不能为空");
        }
        ApiComponent component = getById(request.getId());
        if (component == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "组件不存在");
        }
        // 防环校验：新引用的嵌套组件不能形成环
        checkNoCycle(request.getId(), request.getSteps());

        component.setProjectId(request.getProjectId());
        component.setModuleId(request.getModuleId());
        component.setName(request.getName().trim());
        component.setDescription(trimToNull(request.getDescription()));
        updateById(component);

        // 替换子步骤
        List<ApiComponentStep> oldSteps = componentStepMapper.selectList(
                new QueryWrapper<ApiComponentStep>().eq("component_id", component.getId()));
        for (ApiComponentStep old : oldSteps) {
            componentStepMapper.deleteById(old.getId());
        }
        saveSteps(component.getId(), request.getSteps());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComponent(Long id) {
        ApiComponent component = getById(id);
        if (component == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "组件不存在");
        }
        // 删除子步骤
        List<ApiComponentStep> oldSteps = componentStepMapper.selectList(
                new QueryWrapper<ApiComponentStep>().eq("component_id", id));
        for (ApiComponentStep old : oldSteps) {
            componentStepMapper.deleteById(old.getId());
        }
        removeById(id);
    }

    // ==================== 子步骤 ====================

    private void saveSteps(Long componentId, List<ApiComponentCreateRequest.ComponentStepDTO> steps) {
        if (steps == null || steps.isEmpty()) {
            return;
        }
        for (int i = 0; i < steps.size(); i++) {
            ApiComponentCreateRequest.ComponentStepDTO dto = steps.get(i);
            Integer stepType = dto.getStepType() != null ? dto.getStepType() : 1;
            // 生成变量步骤（stepType=3）：校验生成器存在 + 变量名合法
            String variableName = null;
            Integer regenEachRun = null;
            if (stepType == 2) {
                if (dto.getChildComponentId() == null) {
                    throw new BizException(ResultCode.BAD_REQUEST, "嵌套组件步骤必须选择引用的组件");
                }
            } else if (stepType == 3) {
                if (dto.getGeneratorId() == null) {
                    throw new BizException(ResultCode.BAD_REQUEST, "生成变量步骤必须选择数据生成器");
                }
                if (dataGeneratorMapper.selectById(dto.getGeneratorId()) == null) {
                    throw new BizException(ResultCode.NOT_FOUND, "数据生成器不存在或已被删除");
                }
                variableName = GeneratorEngine.checkVariableName(dto.getVariableName());
                regenEachRun = dto.getRegenEachRun() != null ? dto.getRegenEachRun() : 1;
            } else {
                if (dto.getApiId() == null) {
                    throw new BizException(ResultCode.BAD_REQUEST, "接口步骤必须选择关联接口");
                }
            }
            ApiComponentStep step = new ApiComponentStep();
            step.setComponentId(componentId);
            step.setStepType(stepType);
            step.setApiId(dto.getApiId());
            step.setChildComponentId(dto.getChildComponentId());
            step.setGeneratorId(stepType == 3 ? dto.getGeneratorId() : null);
            step.setVariableName(variableName);
            step.setRegenEachRun(stepType == 3 ? regenEachRun : 1);
            step.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : i + 1);
            step.setStepName(trimToNull(dto.getStepName()));
            step.setRequestOverride(trimToNull(dto.getRequestOverride()));
            step.setAssertions(trimToNull(dto.getAssertions()));
            step.setResponseVar(trimToNull(dto.getResponseVar()));
            step.setIsDisabled(dto.getIsDisabled() != null ? dto.getIsDisabled() : 0);
            step.setContinueOnFail(dto.getContinueOnFail() != null ? dto.getContinueOnFail() : 0);
            step.setDescription(trimToNull(dto.getDescription()));
            componentStepMapper.insert(step);
        }
    }

    /** 防环：校验当前组件不会通过新子步骤间接引用自身 */
    private void checkNoCycle(Long rootComponentId, List<ApiComponentCreateRequest.ComponentStepDTO> steps) {
        if (steps == null) {
            return;
        }
        for (ApiComponentCreateRequest.ComponentStepDTO dto : steps) {
            if (dto.getStepType() != null && dto.getStepType() == 2 && dto.getChildComponentId() != null) {
                Long childId = dto.getChildComponentId();
                if (childId.equals(rootComponentId)) {
                    throw new BizException(ResultCode.BAD_REQUEST, "组件不能嵌套自身");
                }
                // 递归检查子组件的子组件链
                checkDescendants(rootComponentId, childId, 0);
            }
        }
    }

    private void checkDescendants(Long rootComponentId, Long currentComponentId, int depth) {
        if (depth > 10) {
            throw new BizException(ResultCode.BAD_REQUEST, "组合组件嵌套层级过深");
        }
        List<ApiComponentStep> childSteps = componentStepMapper.selectList(
                new QueryWrapper<ApiComponentStep>()
                        .eq("component_id", currentComponentId)
                        .eq("step_type", 2));
        for (ApiComponentStep s : childSteps) {
            Long next = s.getChildComponentId();
            if (next == null) {
                continue;
            }
            if (next.equals(rootComponentId)) {
                throw new BizException(ResultCode.BAD_REQUEST, "组合组件嵌套会形成循环引用");
            }
            checkDescendants(rootComponentId, next, depth + 1);
        }
    }

    private ApiComponentVO toVO(ApiComponent component) {
        return ApiComponentVO.builder()
                .id(component.getId())
                .projectId(component.getProjectId())
                .moduleId(component.getModuleId())
                .name(component.getName())
                .description(component.getDescription())
                .createBy(component.getCreateBy())
                .createTime(component.getCreateTime())
                .updateTime(component.getUpdateTime())
                .steps(null)
                .build();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
