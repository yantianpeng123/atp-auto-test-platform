package com.atp.module.dashboard.service.impl;

import com.atp.module.dashboard.service.DashboardService;
import com.atp.module.dashboard.vo.DashboardStatsVO;
import com.atp.module.execute.entity.Execution;
import com.atp.module.execute.mapper.ExecutionMapper;
import com.atp.module.project.entity.Project;
import com.atp.module.project.mapper.ProjectMapper;
import com.atp.module.testcase.entity.TestCase;
import com.atp.module.testcase.mapper.TestCaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工作台统计服务实现
 * <p>统计范围为全平台（不按项目过滤）。逻辑删除条件由 @TableLogic 自动附加。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProjectMapper projectMapper;
    private final TestCaseMapper testCaseMapper;
    private final ExecutionMapper executionMapper;

    @Override
    public DashboardStatsVO getStats() {
        DashboardStatsVO vo = new DashboardStatsVO();

        // 项目总数 / 用例总数：逻辑删除由 @TableLogic 自动附加 deleted=0
        vo.setProjectCount(projectMapper.selectCount(new QueryWrapper<>()));
        vo.setCaseCount(testCaseMapper.selectCount(new QueryWrapper<>()));

        // 本月执行：create_time >= 当月1号 00:00:00
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        vo.setMonthExecutionCount(
                executionMapper.selectCount(new QueryWrapper<Execution>().ge("create_time", monthStart))
        );

        // 通过率：SUCCESS ÷ (SUCCESS + FAILED)，排除 RUNNING 等中间态
        long finished = executionMapper.selectCount(
                new QueryWrapper<Execution>().in("status", "SUCCESS", "FAILED")
        );
        long success = executionMapper.selectCount(
                new QueryWrapper<Execution>().eq("status", "SUCCESS")
        );
        double passRate = finished == 0 ? 0.0 : Math.round(success * 1000.0 / finished) / 10.0;
        vo.setPassRate(passRate);

        return vo;
    }
}
