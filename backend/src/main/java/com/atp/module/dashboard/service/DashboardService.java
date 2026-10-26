package com.atp.module.dashboard.service;

import com.atp.module.dashboard.vo.DashboardStatsVO;

/**
 * 工作台统计服务
 */
public interface DashboardService {

    /**
     * 获取工作台统计卡片数据（全平台聚合）
     */
    DashboardStatsVO getStats();
}
