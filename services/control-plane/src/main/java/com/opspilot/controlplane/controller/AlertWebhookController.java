package com.opspilot.controlplane.controller;

import com.opspilot.controlplane.dto.alert.AlertWebhookRequest;
import com.opspilot.controlplane.dto.common.ApiResponse;
import com.opspilot.controlplane.dto.incident.IncidentResponse;
import com.opspilot.controlplane.service.AlertIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert Ingestion", description = "High-throughput operational alert webhook ingestion endpoint")
public class AlertWebhookController {

    private final AlertIngestionService alertIngestionService;

    @PostMapping("/webhook")
    @Operation(summary = "Ingest an operational alert via webhook (creates or groups into incident, writes outbox event)")
    public ResponseEntity<ApiResponse<IncidentResponse>> ingestAlert(
            @Valid @RequestBody AlertWebhookRequest request
    ) {
        IncidentResponse response = alertIngestionService.ingestAlert(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Alert ingested successfully", response));
    }
}
