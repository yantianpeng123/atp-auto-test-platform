package com.atp.module.ci.service;

import com.atp.module.ci.dto.CiConfigSaveRequest;
import com.atp.module.ci.dto.CiTriggerRequest;
import com.atp.module.ci.vo.CiConfigVO;
import com.atp.module.ci.vo.CiResultVO;
import com.atp.module.ci.vo.CiTriggerResultVO;

/**
 * CI 集成服务：令牌校验、批次异步触发、结果轮询，以及配置（含令牌）管理。
 */
public interface CiService {

    /** 触发批次执行（外部 CI 携带 X-CI-Token 调用）。立即返回 runId，批次后台异步执行。 */
    CiTriggerResultVO trigger(CiTriggerRequest req, String token);

    /** 轮询某次运行的实时结果（供 CI 判断 build 红绿）。 */
    CiResultVO getResult(Long runId, String token);

    /** 新增/更新 CI 配置，返回配置（新建时含一次性明文令牌）。 */
    CiConfigVO upsertConfig(CiConfigSaveRequest req);

    /** 按项目查询 CI 配置（不含明文令牌）。 */
    CiConfigVO getConfig(Long projectId);

    /** 重新生成令牌，返回新明文令牌（旧令牌立即失效）。 */
    CiConfigVO regenerateToken(Long projectId);
}
