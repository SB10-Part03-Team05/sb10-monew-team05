package com.codeit.monew.domain.notification.event;

import java.util.List;
import java.util.UUID;

public record BulkArticleRegisteredEvent(
    List<InterestArticleCount> interestCounts
) {
  // 방어적 복사본 생성으로 불변성 보장
  public BulkArticleRegisteredEvent {
    interestCounts = (interestCounts == null) ? List.of() : List.copyOf(interestCounts);
  }

  // 기사 알림 생성에 필요한 최소한의 데이터만 묶은 레코드
  public record InterestArticleCount(
      UUID interestId, // 관심사 ID
      String interestName, // 관심사 이름
      long articleCount // 새로 등록된 기사 수
  ) {
    public InterestArticleCount {
      if (interestId == null) {
        throw new IllegalArgumentException("관심사 ID는 null일 수 없습니다.");
      }
      if (articleCount < 0) {
        throw new IllegalArgumentException("기사 수는 0보다 작을 수 없습니다.");
      }
    }
  }
}
