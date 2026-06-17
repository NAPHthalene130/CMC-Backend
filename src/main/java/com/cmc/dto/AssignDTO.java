package com.cmc.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class AssignDTO {
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    private List<Long> countersignUserIds;
    private List<Long> approveUserIds;
    private List<Long> signUserIds;
}
