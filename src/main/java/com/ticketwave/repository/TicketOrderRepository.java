package com.ticketwave.repository;

import com.ticketwave.domain.TicketOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketOrderRepository extends JpaRepository<TicketOrder, UUID> {

    List<TicketOrder> findByUserId(UUID userId);

    Optional<TicketOrder> findByUserIdAndId(UUID userId, UUID id);
}