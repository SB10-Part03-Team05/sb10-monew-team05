package com.codeit.monew.domain.interest.repository;

import com.codeit.monew.domain.interest.entity.Interest;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InterestRepository extends JpaRepository<Interest, UUID> {

  // 유사도 검사용 전체 이름 조회
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i.name FROM Interest i")
  List<String> findAllNamesWithLock();
}
