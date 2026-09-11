package com.atp.module.testcase.service;

import com.atp.module.testcase.dto.CaseCreateRequest;
import com.atp.module.testcase.dto.CaseUpdateRequest;
import com.atp.module.testcase.vo.CaseStepVO;
import com.atp.module.testcase.vo.CaseVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 测试用例服务
 */
public interface CaseService {

    /** 用例分页查询（按项目过滤，可按接口/名称/优先级/状态过滤） */
    IPage<CaseVO> selectCasePage(long page, long size, Long projectId, Long apiId,
                                 String name, Integer level, Integer status);

    /** 查询用例详情（含步骤列表） */
    CaseVO getCaseDetail(Long caseId);

    /** 新增用例（含步骤） */
    void createCase(CaseCreateRequest request, Long userId);

    /** 编辑用例（含步骤，整体替换） */
    void updateCase(CaseUpdateRequest request);

    /** 删除用例（逻辑删除，同时删除步骤） */
    void deleteCase(Long id);

    /** 启用 / 停用 */
    void updateStatus(Long id, Integer status);

    /** 查询用例下的步骤列表 */
    List<CaseStepVO> getCaseSteps(Long caseId);

    /**
     * HAR 包导入用例
     *
     * <p>流程：解析 HAR → 按 (method+path+moduleId) 查重插入接口定义 → 生成用例与步骤。
     * 整个过程在一个事务中。
     *
     * @param moduleId 目标模块
     * @param caseName 用例名称（写入 tb_test_case.name）
     * @param har      HAR 文件
     * @param userId   当前登录用户
     * @return 导入结果（新增接口数、新增步骤数、跳过接口数）
     */
    HarImportResult importHar(Long moduleId, String caseName, MultipartFile har, Long userId);
}
