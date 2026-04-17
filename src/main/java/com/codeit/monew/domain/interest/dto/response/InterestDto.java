package com.codeit.monew.domain.interest.dto.response;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.entity.Keyword;
import java.util.List;
import java.util.UUID;

// 관심사 응답
public record InterestDto(
    UUID id,
    String name,
    List<String> keywords,
    long subscriberCount,
    boolean subscribedByMe
) {
  // 등록 시 사용
  public static InterestDto from(Interest interest) {
    return new InterestDto(
        interest.getId(),
        interest.getName(),
        interest.getKeywords().stream()
            .map(Keyword::getName)
            .toList(),
        interest.getSubscriberCount(),
        false
    );
  }

  // 목록 조회 시 사용
  public static InterestDto from(Interest interest, boolean subscribedByMe) {
    return new InterestDto(
        interest.getId(),
        interest.getName(),
        interest.getKeywords().stream()
            .map(Keyword::getName)
            .toList(),
        interest.getSubscriberCount(),
        subscribedByMe
    );
  }
}
