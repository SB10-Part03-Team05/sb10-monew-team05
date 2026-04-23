package com.codeit.monew.global.event;

import java.util.UUID;

public record InterestUnSubscribedEvent(
    UUID userId,
    UUID interestId
) {

}
