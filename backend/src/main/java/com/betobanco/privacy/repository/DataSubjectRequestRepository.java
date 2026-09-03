package com.betobanco.privacy.repository;

import com.betobanco.privacy.entity.DataSubjectRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DataSubjectRequestRepository extends JpaRepository<DataSubjectRequest, UUID> {

    List<DataSubjectRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
