package com.opspilot.controlplane.dto.incident;

import com.opspilot.controlplane.domain.enums.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentStatusUpdateRequest {

    @NotNull(message = "New status is required")
    private IncidentStatus status;

    private String note;
}
