package com.opspilot.controlplane.dto.auth;

import com.opspilot.controlplane.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private UUID organizationId;
    private String organizationName;
    private String email;
    private String fullName;
    private Role role;
    private boolean active;
}
