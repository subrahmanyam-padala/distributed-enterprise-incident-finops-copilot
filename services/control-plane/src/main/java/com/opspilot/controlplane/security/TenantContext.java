package com.opspilot.controlplane.security;

import com.opspilot.controlplane.domain.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

public class TenantContext {

    private static final ThreadLocal<TenantInfo> CURRENT_TENANT = new ThreadLocal<>();

    @Getter
    @Builder
    public static class TenantInfo {
        private final UUID organizationId;
        private final UUID userId;
        private final Role role;
        private final String email;
    }

    public static void set(TenantInfo tenantInfo) {
        CURRENT_TENANT.set(tenantInfo);
    }

    public static TenantInfo get() {
        return CURRENT_TENANT.get();
    }

    public static UUID getOrganizationId() {
        TenantInfo info = CURRENT_TENANT.get();
        return info != null ? info.getOrganizationId() : null;
    }

    public static UUID getUserId() {
        TenantInfo info = CURRENT_TENANT.get();
        return info != null ? info.getUserId() : null;
    }

    public static Role getRole() {
        TenantInfo info = CURRENT_TENANT.get();
        return info != null ? info.getRole() : null;
    }

    public static String getEmail() {
        TenantInfo info = CURRENT_TENANT.get();
        return info != null ? info.getEmail() : null;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
