package com.orquidea.bank.card.infrastructure;

import com.orquidea.bank.card.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Integer> {

    Optional<Card> findByToken(String token);
}
