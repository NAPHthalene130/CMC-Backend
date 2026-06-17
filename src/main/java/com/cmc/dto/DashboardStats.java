package com.cmc.dto;

import lombok.Data;

@Data
public class DashboardStats {
    private long totalContracts;
    private long pendingTasks;
    private long completedToday;
    private long expiringSoon;
    private long totalUsers;
    private long totalCustomers;
}
