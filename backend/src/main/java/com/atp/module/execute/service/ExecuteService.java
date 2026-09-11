package com.atp.module.execute.service;

import com.atp.module.execute.dto.CaseExecuteRequest;
import com.atp.module.execute.vo.CaseExecuteVO;

/**
 * 用例执行服务。
 */
public interface ExecuteService {

    /**
     * 单用例调试执行（同步）。
     *
     * @param caseId  用例 ID
     * @param request 执行入参（环境 ID）
     * @return 执行汇总结果
     */
    CaseExecuteVO executeCase(Long caseId, CaseExecuteRequest request);

    /**
     * 查询用例最近一次执行记录（持久化数据）。
     *
     * @param caseId 用例 ID
     * @return 最近一次执行结果；无记录时返回 null
     */
    CaseExecuteVO getLatestExecution(Long caseId);
}
