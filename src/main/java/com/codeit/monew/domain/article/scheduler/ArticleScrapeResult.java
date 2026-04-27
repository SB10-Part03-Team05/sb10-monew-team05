package com.codeit.monew.domain.article.scheduler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 기사 수집 결과를 담는 단순한 객체입니다. record를 사용하여 불변성을 보장하며, plus 메서드로 결과를 쉽게 합칠 수 있습니다.
 */
public record ArticleScrapeResult(
    int totalSavedCount,             // 전체 저장된 기사 수
    Map<UUID, InterestInfo> results // 관심사별 결과 (ID -> {이름, 개수})
) implements Serializable {

  // 관심사 이름과 기사 수를 담는 내부 레코드
  public record InterestInfo(String name, long count) implements Serializable {

  }

  // 빈 결과 객체 생성(초기값 또는 실패 시)
  public static ArticleScrapeResult empty() {
    return new ArticleScrapeResult(0, Map.of());
  }

  // 결과 합치기: 현재 결과 + 새로운 결과를 합쳐서 반환
  public ArticleScrapeResult plus(ArticleScrapeResult other) {
    if (other == null) {
      return this;
    }

    // 1. 관심사 맵 복사 (기존 데이터)
    Map<UUID, InterestInfo> mergedMap = new HashMap<>(this.results);

    // 2. 새로운 결과(other)를 하나씩 꺼내어 합침
    other.results.forEach((id, otherInfo) -> {
      InterestInfo myInfo = mergedMap.get(id);
      if (myInfo == null) {
        mergedMap.put(id, otherInfo);
      } else {
        // 이미 있다면 이름은 그대로 두고 숫자만 더함
        mergedMap.put(id, new InterestInfo(myInfo.name(), myInfo.count() + otherInfo.count()));
      }
    });

    // 3. 합산된 전체 개수와 합쳐진 맵으로 새 객체 반환
    return new ArticleScrapeResult(this.totalSavedCount + other.totalSavedCount, mergedMap);
  }
}
