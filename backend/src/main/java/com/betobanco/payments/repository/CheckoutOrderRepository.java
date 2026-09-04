package com.betobanco.payments.repository;

import com.betobanco.payments.entity.CheckoutOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CheckoutOrderRepository extends JpaRepository<CheckoutOrder, UUID> {

    /**
     * Caminho alternativo quando o {@code order_nsu} nao volta reconhecivel.
     *
     * <p>Acontece quando a fatura foi criada fora da plataforma — pelo app da
     * InfinitePay, por exemplo. O slug e o que sobra para reencontrar o pedido.
     */
    Optional<CheckoutOrder> findByInvoiceSlug(String invoiceSlug);
}
