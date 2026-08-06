package com.ticketwave.repository;

import com.ticketwave.domain.Promotion;
import com.ticketwave.domain.PromotionScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    Optional<Promotion> findByCodeIgnoreCase(String code);

    List<Promotion> findByActiveTrueAndValidUntilAfter(LocalDateTime now);

    List<Promotion> findByScope(PromotionScope scope);
}