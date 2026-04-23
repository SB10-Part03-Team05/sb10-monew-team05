package com.codeit.monew.global.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterestSubscribedEvent(
    UUID userId,
    UUID subscriptionId,
    UUID interestId,
    String interestName,
    List<String> interestKeywords,
    Long interestSubscriberCount,
    Instant createdAt
) {

}
