package com.cmc.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.cmc.common.R;
import com.cmc.dto.DashboardStats;
import com.cmc.entity.Contract;
import com.cmc.entity.ContractProcess;
import com.cmc.entity.Customer;
import com.cmc.entity.User;
import com.cmc.mapper.ContractMapper;
import com.cmc.mapper.ContractProcessMapper;
import com.cmc.mapper.CustomerMapper;
import com.cmc.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "仪表盘统计")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final ContractMapper contractMapper;
    private final ContractProcessMapper processMapper;
    private final UserMapper userMapper;
    private final CustomerMapper customerMapper;

    @Operation(summary = "获取仪表盘统计数据")
    @GetMapping("/dashboard")
    public R<DashboardStats> dashboard() {
        long userId = StpUtil.getLoginIdAsLong();

        long totalContracts = contractMapper.selectCount(null);
        long pendingTasks = processMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ContractProcess>()
                        .eq(ContractProcess::getUserId, userId)
                        .eq(ContractProcess::getState, 0)
        );
        long totalUsers = userMapper.selectCount(null);
        long totalCustomers = customerMapper.selectCount(null);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long completedToday = processMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ContractProcess>()
                        .eq(ContractProcess::getUserId, userId)
                        .eq(ContractProcess::getState, 1)
                        .ge(ContractProcess::getTime, todayStart)
        );

        long expiringSoon = contractMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract>()
                        .le(Contract::getEndTime, LocalDate.now().plusDays(30))
                        .ge(Contract::getEndTime, LocalDate.now())
        );

        DashboardStats stats = new DashboardStats();
        stats.setTotalContracts(totalContracts);
        stats.setPendingTasks(pendingTasks);
        stats.setCompletedToday(completedToday);
        stats.setExpiringSoon(expiringSoon);
        stats.setTotalUsers(totalUsers);
        stats.setTotalCustomers(totalCustomers);
        return R.ok(stats);
    }

    @Operation(summary = "获取合同状态分布")
    @GetMapping("/contract-status")
    public R<List<Map<String, Object>>> contractStatus() {
        List<Map<String, Object>> result = new ArrayList<>();
        // Simulate for now - in production, query contract_state table
        // Waiting for contract_state data to be populated
        return R.ok(result);
    }

    @Operation(summary = "获取月度合同趋势")
    @GetMapping("/monthly-trend")
    public R<List<Map<String, Object>>> monthlyTrend() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("month", i + "月");
            item.put("count", 0);
            result.add(item);
        }
        return R.ok(result);
    }

    @Operation(summary = "获取到期预警合同列表")
    @GetMapping("/expiring")
    public R<List<Contract>> expiring() {
        List<Contract> contracts = contractMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract>()
                        .le(Contract::getEndTime, LocalDate.now().plusDays(30))
                        .ge(Contract::getEndTime, LocalDate.now())
                        .orderByAsc(Contract::getEndTime)
        );
        return R.ok(contracts);
    }

    @Operation(summary = "获取待办数量")
    @GetMapping("/pending-count")
    public R<Long> pendingCount() {
        long userId = StpUtil.getLoginIdAsLong();
        long count = processMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ContractProcess>()
                        .eq(ContractProcess::getUserId, userId)
                        .eq(ContractProcess::getState, 0)
        );
        return R.ok(count);
    }
}
