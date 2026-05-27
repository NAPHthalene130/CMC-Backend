package com.cmc.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合同流程待办任务视图对象。
 *
 * @author NAPH130
 */
@Data
public class ProcessTaskVO {
    private Long id;
    private Long contractId;
    private String contractNum;
    private String contractName;
    private Integer type;
    private Integer state;
    private Long userId;
    private String content;
    private LocalDateTime time;
}
