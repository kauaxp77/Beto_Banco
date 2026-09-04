package com.betobanco.leads.repository;

import com.betobanco.leads.entity.LeadEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeadEventRepository extends JpaRepository<LeadEvent, UUID> {

    List<LeadEvent> findByLeadIdOrderByOccurredAtDesc(UUID leadId);
}
