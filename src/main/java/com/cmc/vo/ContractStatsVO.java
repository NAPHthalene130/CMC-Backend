package com.cmc.vo;

import lombok.Data;

/**
 * 合同状态统计视图对象。
 *
 * @author NAPH130
 */
@Data
public class ContractStatsVO {
    private long total;
    private long draft;
    private long countersigned;
    private long finalized;
    private long approved;
    private long signed;
}
