package com.codeit.monew.domain.interest.dto.response;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.entity.Keyword;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

// 관심사 응답
public record InterestDto(
    @Schema(description = "관심사 ID")
    UUID id,

    @Schema(description = "관심사 이름")
    String name,

    @Schema(description = "관심사 키워드 목록")
    List<String> keywords,

    @Schema(description = "구독자 수")
    long subscriberCount,

    @Schema(description = "요청자의 구독 여부")
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
