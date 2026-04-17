package com.codeit.monew.domain.interest.dto.response;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.entity.Keyword;
import com.codeit.monew.domain.interest.entity.Subscription;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SubscriptionDto (
    UUID id,
    UUID interestId,
    String interestName,
    List<String> interestKeywords,
    long interestSubscriberCount,
    LocalDateTime createdAt
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
