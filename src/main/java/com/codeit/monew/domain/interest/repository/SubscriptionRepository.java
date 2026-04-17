package com.codeit.monew.domain.interest.repository;

import com.codeit.monew.domain.interest.entity.Subscription;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  // 중복 구독 확인용
  boolean existsByUserIdAndInterestId(UUID userId, UUID interestId);

  // 구독 취소용
  @Transactional
  @Modifying
  @Query("DELETE FROM Subscription s WHERE s.user.id = :userId AND s.interest.id = :interestId")
  long deleteByUserIdAndInterestId(UUID userId, UUID interestId);

}
