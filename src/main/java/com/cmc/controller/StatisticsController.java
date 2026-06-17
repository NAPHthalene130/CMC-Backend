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
import com.cmc.mapper.ContractStateMapper;
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
    private final ContractStateMapper contractStateMapper;
    private final ContractProcessMapper processMapper;
    private final UserMapper userMapper;
    private final CustomerMapper customerMapper;

    /** 判断当前用户是否为管理员（兼容页面刷新后 session 中 user 为 null 的场景） */
    private boolean isAdmin() {
        long userId = StpUtil.getLoginIdAsLong();
        User sessionUser = (User) StpUtil.getSession().get("user");
        if (sessionUser != null && sessionUser.getRoleId() != null) {
            return sessionUser.getRoleId() == 1;
        }
        User dbUser = userMapper.selectById(userId);
        return dbUser != null && dbUser.getRoleId() != null && dbUser.getRoleId() == 1;
    }

    @Operation(summary = "获取仪表盘统计数据")
    @GetMapping("/dashboard")
    public R<DashboardStats> dashboard() {
        long userId = StpUtil.getLoginIdAsLong();
        boolean admin = isAdmin();

        long totalContracts;
        long expiringSoon;
        if (admin) {
            totalContracts = contractMapper.selectCount(null);
            expiringSoon = contractMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract>()
                            .le(Contract::getEndTime, LocalDate.now().plusDays(30))
                            .ge(Contract::getEndTime, LocalDate.now())
            );
        } else {
            totalContracts = contractMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract>()
                            .and(w -> w.eq(Contract::getUserId, userId)
                                    .or()
                                    .exists("SELECT 1 FROM contract_process cp WHERE cp.contract_id = contract.id AND cp.user_id = " + userId))
            );
            expiringSoon = contractMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract>()
                            .and(w -> w.eq(Contract::getUserId, userId)
                                    .or()
                                    .exists("SELECT 1 FROM contract_process cp WHERE cp.contract_id = contract.id AND cp.user_id = " + userId))
                            .le(Contract::getEndTime, LocalDate.now().plusDays(30))
                            .ge(Contract::getEndTime, LocalDate.now())
            );
        }

        long pendingTasks = processMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ContractProcess>()
                        .eq(ContractProcess::getUserId, userId)
                        .eq(ContractProcess::getState, 0)
        );
        long totalUsers = admin ? userMapper.selectCount(null) : 0;
        long totalCustomers = admin ? customerMapper.selectCount(null) : 0;

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long completedToday = processMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ContractProcess>()
                        .eq(ContractProcess::getUserId, userId)
                        .eq(ContractProcess::getState, 1)
                        .ge(ContractProcess::getTime, todayStart)
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
        long userId = StpUtil.getLoginIdAsLong();
        boolean admin = isAdmin();

        List<Map<String, Object>> rawData = admin
                ? contractStateMapper.countByType()
                : contractStateMapper.countByTypeForUser(userId);
        Map<Integer, String> typeNames = new LinkedHashMap<>();
        typeNames.put(1, "起草中");
        typeNames.put(2, "会签完成");
        typeNames.put(3, "定稿完成");
        typeNames.put(4, "审批完成");
        typeNames.put(5, "签订完成");

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : typeNames.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", entry.getKey());
            item.put("name", entry.getValue());
            long count = 0;
            for (Map<String, Object> row : rawData) {
                if (entry.getKey().equals(row.get("type"))) {
                    count = ((Number) row.get("count")).longValue();
                    break;
                }
            }
            item.put("value", count);
            result.add(item);
        }
        return R.ok(result);
    }

    @Operation(summary = "获取月度合同趋势")
    @GetMapping("/monthly-trend")
    public R<List<Map<String, Object>>> monthlyTrend() {
        List<Map<String, Object>> rawData = contractMapper.monthlyTrend();
        return R.ok(rawData);
    }

    @Operation(summary = "获取到期预警合同列表")
    @GetMapping("/expiring")
    public R<List<Contract>> expiring() {
        long userId = StpUtil.getLoginIdAsLong();
        boolean admin = isAdmin();

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract>()
                        .le(Contract::getEndTime, LocalDate.now().plusDays(30))
                        .ge(Contract::getEndTime, LocalDate.now())
                        .orderByAsc(Contract::getEndTime);
        if (!admin) {
            wrapper.and(w -> w.eq(Contract::getUserId, userId)
                    .or()
                    .exists("SELECT 1 FROM contract_process cp WHERE cp.contract_id = contract.id AND cp.user_id = " + userId));
        }
        List<Contract> contracts = contractMapper.selectList(wrapper);
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
