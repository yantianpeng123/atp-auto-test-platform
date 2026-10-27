package com.atp.module.base.controller;

import com.atp.common.result.Result;
import com.atp.module.base.dto.ApplicationCreateRequest;
import com.atp.module.base.dto.ApplicationUpdateRequest;
import com.atp.module.base.dto.ModuleCreateRequest;
import com.atp.module.base.dto.ModuleUpdateRequest;
import com.atp.module.base.dto.VersionCreateRequest;
import com.atp.module.base.dto.VersionUpdateRequest;
import com.atp.module.base.entity.Application;
import com.atp.module.base.entity.ApplicationModule;
import com.atp.module.base.entity.ApplicationVersion;
import com.atp.module.base.service.ApplicationModuleService;
import com.atp.module.base.service.ApplicationService;
import com.atp.module.base.service.ApplicationVersionService;
import com.atp.module.base.vo.OptionVO;
import com.atp.common.result.PageResult;
import com.atp.module.base.vo.VersionInfoVO;
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

import java.util.List;

/**
 * 基础数据管理接口：工程 / 版本 / 模块
 */
@RestController
@RequestMapping("/api/base")
@RequiredArgsConstructor
public class BaseDataController {

    private final ApplicationService applicationService;
    private final ApplicationVersionService versionService;
    private final ApplicationModuleService moduleService;

    /** 工程下拉选项（可按项目过滤） */
    @GetMapping("/project/options")
    public Result<List<OptionVO>> projectOptions(@RequestParam(required = false) Long projectId) {
        List<OptionVO> list = applicationService.lambdaQuery()
                .eq(projectId != null, Application::getProjectId, projectId)
                .list().stream()
                .map(a -> OptionVO.builder().id(a.getId()).name(a.getName()).build())
                .toList();
        return Result.success(list);
    }

    /** 版本下拉选项（可按工程过滤） */
    @GetMapping("/version/options")
    public Result<List<OptionVO>> versionOptions(@RequestParam(required = false) Long applicationId) {
        List<OptionVO> list = versionService.lambdaQuery()
                .eq(applicationId != null, ApplicationVersion::getApplicationId, applicationId)
                .list().stream()
                .map(v -> OptionVO.builder().id(v.getId()).name(v.getName()).build())
                .toList();
        return Result.success(list);
    }

    /** 模块下拉选项（可按版本过滤） */
    @GetMapping("/module/options")
    public Result<List<OptionVO>> moduleOptions(@RequestParam(required = false) Long versionId) {
        List<OptionVO> list = moduleService.lambdaQuery()
                .eq(versionId != null, ApplicationModule::getVersionId, versionId)
                .list().stream()
                .map(m -> OptionVO.builder().id(m.getId()).name(m.getName()).build())
                .toList();
        return Result.success(list);
    }

    /** 新增工程 */
    @PostMapping("/project")
    public Result<Void> createProject(@Valid @RequestBody ApplicationCreateRequest request) {
        applicationService.createApplication(request);
        return Result.ok("工程新增成功");
    }

    /** 新增版本 */
    @PostMapping("/version")
    public Result<Void> createVersion(@Valid @RequestBody VersionCreateRequest request) {
        versionService.createVersion(request.getApplicationId(), request.getName(), request.getDescription());
        return Result.ok("版本新增成功");
    }

    /** 新增模块 */
    @PostMapping("/module")
    public Result<Void> createModule(@Valid @RequestBody ModuleCreateRequest request) {
        moduleService.createModules(request.getVersionId(), request.getNames(), request.getDescription());
        return Result.ok("模块新增成功");
    }

    /** 工程版本信息分页查询（可按项目/工程/版本/模块过滤） */
    @GetMapping("/version/list")
    public Result<PageResult<VersionInfoVO>> versionList(@RequestParam(required = false) Long projectId,
                                                         @RequestParam(required = false) Long applicationId,
                                                         @RequestParam(required = false) Long versionId,
                                                         @RequestParam(required = false) Long moduleId,
                                                         @RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size) {
        IPage<VersionInfoVO> result =
                moduleService.selectVersionInfoPage(page, size, projectId, applicationId, versionId, moduleId);
        PageResult<VersionInfoVO> pageResult = PageResult.<VersionInfoVO>builder()
                .records(result.getRecords())
                .total(result.getTotal())
                .page(result.getCurrent())
                .size(result.getSize())
                .build();
        return Result.success(pageResult);
    }

    /** 重命名模块 */
    @PutMapping("/module")
    public Result<Void> renameModule(@Valid @RequestBody ModuleUpdateRequest request) {
        moduleService.renameModule(request.getId(), request.getName());
        return Result.ok("模块更新成功");
    }

    /** 删除模块（逻辑删除） */
    @DeleteMapping("/module/{id}")
    public Result<Void> deleteModule(@PathVariable Long id) {
        moduleService.removeById(id);
        return Result.ok("模块删除成功");
    }

    /** 重命名工程 */
    @PutMapping("/project")
    public Result<Void> renameApplication(@Valid @RequestBody ApplicationUpdateRequest request) {
        applicationService.renameApplication(request.getId(), request.getName());
        return Result.ok("工程更新成功");
    }

    /** 删除工程（级联逻辑删除其版本与模块） */
    @DeleteMapping("/project/{id}")
    public Result<Void> deleteApplication(@PathVariable Long id) {
        applicationService.removeApplication(id);
        return Result.ok("工程删除成功");
    }

    /** 重命名版本 */
    @PutMapping("/version")
    public Result<Void> renameVersion(@Valid @RequestBody VersionUpdateRequest request) {
        versionService.renameVersion(request.getId(), request.getName());
        return Result.ok("版本更新成功");
    }

    /** 删除版本（级联逻辑删除其模块） */
    @DeleteMapping("/version/{id}")
    public Result<Void> deleteVersion(@PathVariable Long id) {
        versionService.removeVersion(id);
        return Result.ok("版本删除成功");
    }
}
