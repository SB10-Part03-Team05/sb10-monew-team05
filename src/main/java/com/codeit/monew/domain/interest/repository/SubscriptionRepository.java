package com.codeit.monew.domain.interest.repository;

import com.codeit.monew.domain.interest.entity.Subscription;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  // 중복 구독 확인용
  boolean existsByUserIdAndInterestId(UUID userId, UUID interestId);

  // 구독 취소용
  void deleteByUserIdAndInterestId(UUID userId, UUID interestId);

}
