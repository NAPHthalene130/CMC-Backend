package com.cmc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办任务 VO —— 合并 contract_process + contract + customer 信息
 */
@Data
public class PendingTaskVO {
    // ===== ContractProcess 字段 =====
    private Long id;
    private Long contractId;
    private Integer type;
    private Integer state;
    private Long userId;
    private String content;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime time;

    // ===== Contract 关联字段 =====
    private String contractNum;
    private String contractName;
    private String contractContent;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // ===== User 关联字段 =====
    private String username;

    // ===== Customer 关联字段 =====
    private String customerName;
}
