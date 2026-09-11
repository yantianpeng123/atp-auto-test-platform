package com.atp.module.base.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.base.dto.ApiCreateRequest;
import com.atp.module.base.dto.ApiUpdateRequest;
import com.atp.module.base.entity.ApiDefinition;
import com.atp.module.base.entity.ApplicationModule;
import com.atp.module.base.mapper.ApiDefinitionMapper;
import com.atp.module.base.service.ApiDefinitionService;
import com.atp.module.base.service.ApplicationModuleService;
import com.atp.module.base.util.JarApiParser;
import com.atp.module.base.vo.ApiVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 接口定义服务实现
 */
@Service
@RequiredArgsConstructor
public class ApiDefinitionServiceImpl extends ServiceImpl<ApiDefinitionMapper, ApiDefinition>
        implements ApiDefinitionService {

    private final ApplicationModuleService moduleService;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<ApiVO> selectApiPage(long page, long size, Long projectId, Long applicationId,
                                      Long versionId, Long moduleId, String name, String path) {
        Page<ApiVO> pageParam = new Page<>(page, size);
        return baseMapper.selectApiPage(pageParam, projectId, applicationId, versionId, moduleId, name, path);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createApi(ApiCreateRequest request) {
        ApplicationModule module = moduleService.getById(request.getModuleId());
        if (module == null) {
            throw new BizException(ResultCode.MODULE_NOT_FOUND);
        }
        if (apIsone(request.getModuleId(), request.getMethod(), request.getPath())) {
            throw new BizException(ResultCode.API_NAME_EXISTS);
        }
        ApiDefinition api = new ApiDefinition();
        api.setSourceFlag("手动添加");
        api.setModuleId(request.getModuleId());
        api.setName(request.getName());
        api.setMethod(request.getMethod());
        api.setPath(request.getPath());
        api.setHeaders(normalizeJson(request.getHeaders(), "请求头模板"));
        api.setBody(normalizeJson(request.getBody(), "请求体模板"));
        api.setDescription(trimToNull(request.getDescription()));
        save(api);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApi(ApiUpdateRequest request) {
        ApiDefinition api = getById(request.getId());
        if (api == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "接口不存在");
        }
        // 如果 method/path 有变，校验新组合在模块内不重复
        if (!api.getMethod().equals(request.getMethod()) || !api.getPath().equals(request.getPath())) {
            if (apIsone(api.getModuleId(), request.getMethod(), request.getPath())) {
                throw new BizException(ResultCode.API_NAME_EXISTS);
            }
        }
        api.setName(request.getName());
        api.setMethod(request.getMethod());
        api.setPath(request.getPath());
        api.setHeaders(normalizeJson(request.getHeaders(), "请求头模板"));
        api.setBody(normalizeJson(request.getBody(), "请求体模板"));
        api.setDescription(trimToNull(request.getDescription()));
        updateById(api);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApi(Long id) {
        ApiDefinition api = getById(id);
        if (api == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "接口不存在");
        }
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importApiFromJar(Long moduleId, InputStream jarInputStream) {
        ApplicationModule module = moduleService.getById(moduleId);
        if (module == null) {
            throw new BizException(ResultCode.MODULE_NOT_FOUND);
        }

        List<JarApiParser.ParsedApi> apis;
        try {
            apis = new JarApiParser().parse(jarInputStream);
        } catch (IOException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "Jar 包解析失败：" + e.getMessage());
        }
        int inserted = 0;
        for (JarApiParser.ParsedApi api : apis) {
            if (apIsone(moduleId, api.method(), api.path())) {
                continue;
            }
            ApiDefinition def = new ApiDefinition();
            def.setModuleId(moduleId);
            def.setName(api.name());
            def.setMethod(api.method());
            def.setPath(api.path());
            def.setSourceFlag("Jar导入");
            save(def);
            inserted++;
        }
        return inserted;
    }

    /**
     * 接口唯一性：同一模块下请求方式+请求路径不可重复
     */
    @Override
    public Boolean apIsone(long moduleId, String method, String path) {
        long count = lambdaQuery()
                .eq(ApiDefinition::getModuleId, moduleId)
                .eq(ApiDefinition::getMethod, method)
                .eq(ApiDefinition::getPath, path)
                .count();
        return count > 0;
    }

    /**
     * JSON 字段落库前校验，非法 JSON 直接拒绝
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

}
