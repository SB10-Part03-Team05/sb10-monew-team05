package com.codeit.monew.domain.interest.repository;

import com.codeit.monew.domain.interest.dto.response.CursorPageResponseInterestDto;
import java.time.Instant;
import java.util.UUID;

public interface InterestRepositoryCustom {
  CursorPageResponseInterestDto findInterests(
      String searchKeyword,
      String orderBy,
      String direction,
      String cursor,
      Instant after,
      int limit,
      UUID userId
  );

}
