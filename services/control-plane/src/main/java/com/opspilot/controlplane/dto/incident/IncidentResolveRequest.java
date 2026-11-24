package com.opspilot.controlplane.dto.incident;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentResolveRequest {

    @NotBlank(message = "Resolution summary is required")
    private String resolutionSummary;
}
