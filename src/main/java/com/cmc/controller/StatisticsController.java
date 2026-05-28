package com.cmc.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmc.common.R;
import com.cmc.dto.DashboardStats;
import com.cmc.entity.Contract;
import com.cmc.entity.ContractProcess;
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
import java.time.YearMonth;
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

    @Operation(summary = "获取仪表盘统计数据")
    @GetMapping("/dashboard")
    public R<DashboardStats> dashboard() {
        long userId = StpUtil.getLoginIdAsLong();

        long totalContracts = contractMapper.selectCount(null);
        long pendingTasks = visiblePendingCount(userId);
        long totalUsers = userMapper.selectCount(null);
        long totalCustomers = customerMapper.selectCount(null);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long completedToday = processMapper.selectCount(
                new LambdaQueryWrapper<ContractProcess>()
                        .eq(ContractProcess::getUserId, userId)
                        .eq(ContractProcess::getState, 1)
                        .ge(ContractProcess::getTime, todayStart)
        );

        long expiringSoon = contractMapper.selectCount(
                new LambdaQueryWrapper<Contract>()
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
        List<Map<String, Object>> rawData = contractStateMapper.countByType();
        Map<Integer, String> typeNames = new LinkedHashMap<>();
        typeNames.put(1, "待会签");
        typeNames.put(2, "待定稿");
        typeNames.put(3, "待审批");
        typeNames.put(4, "待签订");
        typeNames.put(5, "已签订");

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
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(5);
        LocalDateTime startTime = startMonth.atDay(1).atStartOfDay();

        List<Contract> contracts = contractMapper.selectList(
                new LambdaQueryWrapper<Contract>()
                        .ge(Contract::getCreateTime, startTime)
        );

        Map<String, Long> countByMonth = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            countByMonth.put(startMonth.plusMonths(i).toString(), 0L);
        }

        for (Contract contract : contracts) {
            LocalDateTime createTime = contract.getCreateTime();
            if (createTime == null) {
                continue;
            }
            String month = YearMonth.from(createTime).toString();
            if (countByMonth.containsKey(month)) {
                countByMonth.put(month, countByMonth.get(month) + 1);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : countByMonth.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("month", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        return R.ok(result);
    }

    @Operation(summary = "获取到期预警合同列表")
    @GetMapping("/expiring")
    public R<List<Contract>> expiring() {
        List<Contract> contracts = contractMapper.selectList(
                new LambdaQueryWrapper<Contract>()
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
        return R.ok(visiblePendingCount(userId));
    }

    private long visiblePendingCount(long userId) {
        List<ContractProcess> processes = processMapper.selectList(
                new LambdaQueryWrapper<ContractProcess>()
                        .eq(ContractProcess::getUserId, userId)
                        .eq(ContractProcess::getState, 0)
        );
        return processes.stream().filter(process -> {
            Contract contract = contractMapper.selectById(process.getContractId());
            if (contract == null) {
                return false;
            }
            Integer state = contract.getState();
            Integer type = process.getType();
            return (type == 1 && Objects.equals(state, 1))
                    || (type == 2 && Objects.equals(state, 3))
                    || (type == 3 && Objects.equals(state, 4));
        }).count();
    }
}
