package com.cmc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
public class ContractDTO {
    @NotBlank(message = "合同名称不能为空")
    private String name;

    private Long customerId;

    @NotNull(message = "开始时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate beginTime;

    @NotNull(message = "结束时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endTime;

    @NotBlank(message = "合同内容不能为空")
    private String content;
}
