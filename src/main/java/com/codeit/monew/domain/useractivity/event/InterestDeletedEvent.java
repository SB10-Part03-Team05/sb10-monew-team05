package com.codeit.monew.domain.useractivity.event;

import java.util.UUID;

public record InterestDeletedEvent(
    UUID interestId
) {

}
