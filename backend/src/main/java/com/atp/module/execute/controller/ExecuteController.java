package com.atp.module.execute.controller;

import com.atp.common.result.Result;
import com.atp.module.execute.dto.CaseExecuteRequest;
import com.atp.module.execute.service.ExecuteService;
import com.atp.module.execute.vo.CaseExecuteVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用例执行接口。
 */
@RestController
@RequestMapping("/api/execute")
@RequiredArgsConstructor
public class ExecuteController {

    private final ExecuteService executeService;

    /** 单用例调试执行 */
    @PostMapping("/case/{caseId}")
    public Result<CaseExecuteVO> executeCase(@PathVariable Long caseId,
                                             @Valid @RequestBody CaseExecuteRequest request) {
        return Result.success(executeService.executeCase(caseId, request));
    }
}
