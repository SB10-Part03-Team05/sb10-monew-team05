package com.codeit.monew.domain.useractivity.event;

import java.util.UUID;

public record InterestUnSubscribedEvent(
    UUID userId,
    UUID interestId
) {

}
