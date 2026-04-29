package com.codeit.monew.domain.interest.dto.response;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.entity.Keyword;
import com.codeit.monew.domain.interest.entity.Subscription;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SubscriptionDto (
    @Schema(description = "구독 정보 ID")
    UUID id,

    @Schema(description = "관심사 ID")
    UUID interestId,

    @Schema(description = "관심사 이름")
    String interestName,

    @Schema(description = "관련 키워드 목록")
    List<String> interestKeywords,

    @Schema(description = "구독자 수")
    long interestSubscriberCount,

    @Schema(description = "구독한 날짜")
    Instant createdAt
) {
  public static SubscriptionDto from(Subscription subscription) {
    Interest interest = subscription.getInterest();
    return new SubscriptionDto(
        subscription.getId(),
        interest.getId(),
        interest.getName(),
        interest.getKeywords().stream()
            .map(Keyword::getName)
            .toList(),
        interest.getSubscriberCount(),
        subscription.getCreatedAt()
    );
  }
}
