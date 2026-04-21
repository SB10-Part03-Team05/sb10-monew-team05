package com.codeit.monew.domain.interest.repository;

import com.codeit.monew.domain.interest.entity.Interest;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestRepository extends JpaRepository<Interest, UUID>, InterestRepositoryCustom {

  // 유사도 검사용 전체 이름 조회
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i.name FROM Interest i")
  List<String> findAllNamesWithLock();

  // 구독자 수 atomic 업데이트
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Interest i SET i.subscriberCount = i.subscriberCount + 1 WHERE i.id = :interestId")
  int incrementSubscriberCount(@Param("interestId") UUID interestId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Interest i SET i.subscriberCount = i.subscriberCount - 1 WHERE i.id = :id AND i.subscriberCount > 0")
  int decrementSubscriberCount(@Param("id") UUID id);
}
