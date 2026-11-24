package com.opspilot.controlplane.dto.finops;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudCostSummary {
    private LocalDate date;
    private String serviceName;
    private String provider;
    private BigDecimal amount;
    private String currency;
}
