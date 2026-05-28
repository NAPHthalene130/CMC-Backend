package com.cmc.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PendingTaskDTO {
    private Long id;
    private Long processId;
    private Long contractId;
    private String contractNum;
    private String contractName;
    private Long customerId;
    private String content;
    private String processContent;
    private Integer type;
    private Integer state;
    private Long userId;
    private LocalDate beginTime;
    private LocalDate endTime;
    private LocalDateTime time;
    private LocalDateTime createTime;
}
