package com.betobanco.payments.repository;

import com.betobanco.payments.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByProviderAndProviderTransactionId(String provider, String transactionId);

    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Payment> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByStatus(String status);
}
