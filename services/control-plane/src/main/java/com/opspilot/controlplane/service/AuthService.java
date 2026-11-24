package com.opspilot.controlplane.service;

import com.opspilot.controlplane.domain.entity.Organization;
import com.opspilot.controlplane.domain.entity.User;
import com.opspilot.controlplane.domain.enums.Role;
import com.opspilot.controlplane.dto.auth.*;
import com.opspilot.controlplane.exception.AuthenticationFailedException;
import com.opspilot.controlplane.exception.DuplicateResourceException;
import com.opspilot.controlplane.exception.ResourceNotFoundException;
import com.opspilot.controlplane.repository.OrganizationRepository;
import com.opspilot.controlplane.repository.UserRepository;
import com.opspilot.controlplane.security.JwtTokenProvider;
import com.opspilot.controlplane.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;

    @Value("${opspilot.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting organization registration for slug: {}", request.getOrganizationSlug());

        if (organizationRepository.existsBySlug(request.getOrganizationSlug())) {
            throw new DuplicateResourceException(
                    "Organization with slug '" + request.getOrganizationSlug() + "' already exists"
            );
        }

        // 1. Create Organization
        Organization organization = Organization.builder()
                .name(request.getOrganizationName())
                .slug(request.getOrganizationSlug())
                .plan("free")
                .active(true)
                .build();
        organization = organizationRepository.save(organization);

        // 2. Create Initial Admin User
        User adminUser = User.builder()
                .organization(organization)
                .email(request.getAdminEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getAdminPassword()))
                .fullName(request.getAdminFullName())
                .role(Role.ADMIN)
                .active(true)
                .build();
        adminUser = userRepository.save(adminUser);

        // 3. Generate JWT
        String token = tokenProvider.generateToken(
                adminUser.getId(),
                organization.getId(),
                adminUser.getEmail(),
                adminUser.getRole()
        );

        // 4. Audit Log
        auditLogService.record(
                organization.getId(),
                adminUser.getId(),
                "ORG_REGISTERED",
                "Organization",
                organization.getId().toString(),
                "{\"name\":\"" + organization.getName() + "\",\"slug\":\"" + organization.getSlug() + "\"}",
                null,
                null
        );

        return buildAuthResponse(token, adminUser, organization);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("Processing login request for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid credentials"));

        if (!user.getActive()) {
            throw new AuthenticationFailedException("User account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid credentials");
        }

        Organization org = user.getOrganization();
        if (!org.getActive()) {
            throw new AuthenticationFailedException("Organization account is deactivated");
        }

        String token = tokenProvider.generateToken(
                user.getId(),
                org.getId(),
                user.getEmail(),
                user.getRole()
        );

        auditLogService.record(
                org.getId(),
                user.getId(),
                "USER_LOGIN",
                "User",
                user.getId().toString(),
                "{\"email\":\"" + user.getEmail() + "\"}",
                null,
                null
        );

        return buildAuthResponse(token, user, org);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        UUID userId = TenantContext.getUserId();
        if (userId == null) {
            throw new AuthenticationFailedException("Unauthenticated request");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToUserResponse(user);
    }

    private AuthResponse buildAuthResponse(String token, User user, Organization organization) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(jwtExpirationMs)
                .user(mapToUserResponse(user))
                .organization(mapToOrgResponse(organization))
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .organizationId(user.getOrganization().getId())
                .organizationName(user.getOrganization().getName())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.getActive())
                .build();
    }

    private OrganizationResponse mapToOrgResponse(Organization org) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .slug(org.getSlug())
                .plan(org.getPlan())
                .build();
    }
}
