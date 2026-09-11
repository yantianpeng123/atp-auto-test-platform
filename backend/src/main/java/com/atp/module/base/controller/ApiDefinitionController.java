package com.atp.module.base.controller;

import com.atp.common.result.Result;
import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.common.result.PageResult;
import com.atp.module.base.dto.ApiCreateRequest;
import com.atp.module.base.dto.ApiUpdateRequest;
import com.atp.module.base.service.ApiDefinitionService;
import com.atp.module.base.vo.ApiVO;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 接口定义接口
 */
@RestController
@RequestMapping("/api/base/api")
@RequiredArgsConstructor
public class ApiDefinitionController {

    private final ApiDefinitionService apiDefinitionService;

    /** 接口列表分页查询（可按项目/工程/版本/模块/名称/路径过滤） */
    @GetMapping("/list")
    public Result<PageResult<ApiVO>> list(@RequestParam(required = false) Long projectId,
                                          @RequestParam(required = false) Long applicationId,
                                          @RequestParam(required = false) Long versionId,
                                          @RequestParam(required = false) Long moduleId,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(required = false) String path,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size) {
        IPage<ApiVO> result = apiDefinitionService.selectApiPage(
                page, size, projectId, applicationId, versionId, moduleId, name, path);
        PageResult<ApiVO> pageResult = PageResult.<ApiVO>builder()
                .records(result.getRecords())
                .total(result.getTotal())
                .page(result.getCurrent())
                .size(result.getSize())
                .build();
        return Result.success(pageResult);
    }

    /** 新增接口 */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ApiCreateRequest request) {
        apiDefinitionService.createApi(request);
        return Result.ok("接口新增成功");
    }

    /** 编辑接口 */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ApiUpdateRequest request) {
        apiDefinitionService.updateApi(request);
        return Result.ok("接口修改成功");
    }

    /** 删除接口 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        apiDefinitionService.deleteApi(id);
        return Result.ok("接口删除成功");
    }

    /** Jar 包导入接口 */
    @PostMapping("/import")
    public Result<Integer> importJar(@RequestParam("moduleId") Long moduleId,
                                     @RequestParam("jar") MultipartFile jar) {
        try (InputStream in = jar.getInputStream()) {
            int count = apiDefinitionService.importApiFromJar(moduleId, in);
            return Result.success(count);
        } catch (IOException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件读取失败");
        }
    }
}
