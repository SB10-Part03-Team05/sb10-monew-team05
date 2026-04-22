package com.codeit.monew.domain.interest.repository;

import com.codeit.monew.domain.interest.entity.Keyword;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface KeywordRepository extends JpaRepository<Keyword, UUID> {

  // 관심사 수정 시 기존 키워드 전체 삭제용
  void deleteAllByInterestId(UUID interestId);

  @Query("SELECT k FROM Keyword k JOIN FETCH k.interest")
  List<Keyword> findAllWithInterest();
}
