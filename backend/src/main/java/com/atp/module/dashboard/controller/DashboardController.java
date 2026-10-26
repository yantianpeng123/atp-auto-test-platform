package com.atp.module.dashboard.controller;

import com.atp.common.result.Result;
import com.atp.module.dashboard.service.DashboardService;
import com.atp.module.dashboard.vo.DashboardStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作台统计接口
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 获取统计卡片数据 */
    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        return Result.success(dashboardService.getStats());
    }
}
