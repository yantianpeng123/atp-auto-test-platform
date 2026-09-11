package com.atp.module.dataset.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.dataset.dto.DatasetItemDTO;
import com.atp.module.dataset.dto.DatasetTemplateRequest;
import com.atp.module.dataset.service.DatasetService;
import com.atp.module.dataset.vo.DatasetItemVO;
import com.atp.module.dataset.vo.DatasetTemplateVO;
import com.atp.module.testcase.entity.DatasetItem;
import com.atp.module.testcase.entity.DatasetTemplate;
import com.atp.module.testcase.entity.TestCase;
import com.atp.module.testcase.mapper.DatasetItemMapper;
import com.atp.module.testcase.mapper.DatasetTemplateMapper;
import com.atp.module.testcase.mapper.TestCaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据源（模板 + 数据项）服务实现。
 */
@Service
@RequiredArgsConstructor
public class DatasetServiceImpl implements DatasetService {

    private final DatasetTemplateMapper datasetTemplateMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final TestCaseMapper testCaseMapper;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<DatasetTemplateVO> selectPage(long page, long size, Long caseId, String name) {
        QueryWrapper<DatasetTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq(caseId != null, "case_id", caseId);
        wrapper.like(StringUtils.hasText(name), "name", name);
        wrapper.orderByDesc("update_time");

        IPage<DatasetTemplate> pageResult = datasetTemplateMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, String> caseNameMap = loadCaseNameMap(pageResult.getRecords());

        List<DatasetTemplateVO> voList = pageResult.getRecords().stream()
                .map(t -> toVO(t, caseNameMap.get(t.getCaseId()), null))
                .collect(Collectors.toList());

        Page<DatasetTemplateVO> voPage = new Page<>(page, size, pageResult.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public DatasetTemplateVO getDetail(Long id) {
        DatasetTemplate template = datasetTemplateMapper.selectById(id);
        if (template == null) {
            throw new BizException(ResultCode.NOT_FOUND, "数据源不存在");
        }
        return toVO(template, loadCaseName(template.getCaseId()), loadItems(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(DatasetTemplateRequest request) {
        DatasetTemplate template = new DatasetTemplate();
        template.setCaseId(request.getCaseId());
        template.setName(request.getName().trim());
        template.setCreatorName(trimToNull(request.getCreatorName()));
        template.setKeys(normalizeJson(request.getKeys(), "字段定义"));
        datasetTemplateMapper.insert(template);
        saveItems(template.getId(), request.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DatasetTemplateRequest request) {
        if (request.getId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "数据源ID不能为空");
        }
        DatasetTemplate template = datasetTemplateMapper.selectById(request.getId());
        if (template == null) {
            throw new BizException(ResultCode.NOT_FOUND, "数据源不存在");
        }
        template.setCaseId(request.getCaseId());
        template.setName(request.getName().trim());
        template.setCreatorName(trimToNull(request.getCreatorName()));
        template.setKeys(normalizeJson(request.getKeys(), "字段定义"));
        datasetTemplateMapper.updateById(template);
        // 整体替换数据项：逻辑删除旧的，再插入新的
        datasetItemMapper.delete(new QueryWrapper<DatasetItem>().eq("template_id", template.getId()));
        saveItems(template.getId(), request.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        datasetTemplateMapper.deleteById(id);
        datasetItemMapper.delete(new QueryWrapper<DatasetItem>().eq("template_id", id));
    }

    // ==================== 工具 ====================

    private void saveItems(Long templateId, List<DatasetItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        int order = 1;
        for (DatasetItemDTO dto : items) {
            DatasetItem item = new DatasetItem();
            item.setTemplateId(templateId);
            item.setData(normalizeJson(dto.getData(), "数据项"));
            item.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : order);
            datasetItemMapper.insert(item);
            order++;
        }
    }

    private List<DatasetItemVO> loadItems(Long templateId) {
        List<DatasetItem> items = datasetItemMapper.selectList(
                new QueryWrapper<DatasetItem>()
                        .eq("template_id", templateId)
                        .orderByAsc("sort_order"));
        return items.stream()
                .map(i -> DatasetItemVO.builder()
                        .id(i.getId())
                        .templateId(i.getTemplateId())
                        .data(i.getData())
                        .sortOrder(i.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private Map<Long, String> loadCaseNameMap(List<DatasetTemplate> templates) {
        List<Long> caseIds = templates.stream()
                .map(DatasetTemplate::getCaseId)
                .distinct()
                .collect(Collectors.toList());
        if (caseIds.isEmpty()) {
            return Map.of();
        }
        return testCaseMapper.selectBatchIds(caseIds).stream()
                .collect(Collectors.toMap(TestCase::getId, TestCase::getName));
    }

    private String loadCaseName(Long caseId) {
        TestCase testCase = testCaseMapper.selectById(caseId);
        return testCase == null ? null : testCase.getName();
    }

    private DatasetTemplateVO toVO(DatasetTemplate template, String caseName, List<DatasetItemVO> items) {
        return DatasetTemplateVO.builder()
                .id(template.getId())
                .caseId(template.getCaseId())
                .caseName(caseName)
                .name(template.getName())
                .keys(template.getKeys())
                .creatorName(template.getCreatorName())
                .createTime(template.getCreateTime())
                .updateTime(template.getUpdateTime())
                .items(items)
                .build();
    }

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
}
