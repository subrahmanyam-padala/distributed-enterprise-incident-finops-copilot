package com.opspilot.controlplane.repository;

import com.opspilot.controlplane.domain.entity.CloudCostItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CloudCostItemRepository extends JpaRepository<CloudCostItem, UUID> {

    List<CloudCostItem> findByOrganizationIdAndUsageDateBetweenOrderByUsageDateAsc(
            UUID organizationId, LocalDate startDate, LocalDate endDate);

    List<CloudCostItem> findByOrganizationIdAndServiceNameAndUsageDateBetweenOrderByUsageDateAsc(
            UUID organizationId, String serviceName, LocalDate startDate, LocalDate endDate);

    @Query("SELECT DISTINCT c.serviceName FROM CloudCostItem c WHERE c.organizationId = :organizationId")
    List<String> findDistinctServiceNamesByOrganizationId(UUID organizationId);
}
