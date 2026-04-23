package com.codeit.monew.domain.notification.event;

import java.util.List;
import java.util.UUID;

public record BulkArticleRegisteredEvent(
    List<InterestArticleCount> interestCounts
) {
  // 기사 알림 생성에 필요한 최소한의 데이터만 묶은 레코드
  public record InterestArticleCount(
      UUID interestId, // 관심사 ID
      String interestName, // 관심사 이름
      long articleCount // 새로 등록된 기사 수
  ) {}
}
