package com.cmc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProcessDTO {
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    @NotBlank(message = "操作内容不能为空")
    private String content;

    private Boolean approved;
}
