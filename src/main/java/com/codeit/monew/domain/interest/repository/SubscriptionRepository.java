package com.codeit.monew.domain.interest.repository;

import com.codeit.monew.domain.interest.entity.Subscription;
import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  // 중복 구독 확인용
  boolean existsByUserIdAndInterestId(@Param("userId") UUID userId,
      @Param("interestId") UUID interestId);

  // 구독 취소용
  @Transactional
  @Modifying
  @Query("DELETE FROM Subscription s WHERE s.user.id = :userId AND s.interest.id = :interestId")
  int deleteByUserIdAndInterestId(UUID userId, UUID interestId);

  List<Subscription> findByInterestIdIn(Collection<UUID> interestIds);

}
