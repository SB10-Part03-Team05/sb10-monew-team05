package com.codeit.monew.domain.article.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.notification.event.BulkArticleRegisteredEvent;
import com.codeit.monew.domain.notification.event.BulkArticleRegisteredEvent.InterestArticleCount;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ArticleScrapeBatchNotificationPublisherTest {

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private ArticleScrapeBatchNotificationPublisher publisher;

  @Test
  @DisplayName("전체 결과가 null이면 이벤트를 발행하지 않는다")
  void publish_if_null_does_not_publish() {
    // Given: 결과 데이터가 null인 경우
    ArticleScrapeResult totalResult = null;

    // When: 알림 발행 여부 확인 로직 실행
    publisher.publishIfNeeded(totalResult);

    // Then: 이벤트 발행이 한 번도 호출되지 않아야 함
    verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("저장된 기사 수가 0개이면 이벤트를 발행하지 않는다")
  void publish_if_zero_does_not_publish() {
    // Given: 저장된 기사 수가 0인 빈 결과 객체 생성
    ArticleScrapeResult emptyResult = ArticleScrapeResult.empty();

    // When: 알림 발행 여부 확인 로직 실행
    publisher.publishIfNeeded(emptyResult);

    // Then: 이벤트 발행이 호출되지 않아야 함
    verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("저장된 기사가 있으면 각 관심사별 개수를 포함한 이벤트를 발행한다")
  void publish_if_positive_publishes_event_with_interest_counts() {
    // Given: 두 개의 관심사 데이터가 포함된 결과 생성
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    ArticleScrapeResult totalResult = new ArticleScrapeResult(
        3,
        Map.of(
            id1, new ArticleScrapeResult.InterestInfo("IT", 1),
            id2, new ArticleScrapeResult.InterestInfo("AUTO", 2)
        )
    );

    // When: 알림 발행 여부 확인 로직 실행
    publisher.publishIfNeeded(totalResult);

    // Then: 이벤트가 1회 발행되었는지 확인하고 데이터를 검증
    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher, times(1)).publishEvent(captor.capture());

    // 발행된 이벤트 타입이 BulkArticleRegisteredEvent인지 확인
    assertInstanceOf(BulkArticleRegisteredEvent.class, captor.getValue());

    BulkArticleRegisteredEvent event = (BulkArticleRegisteredEvent) captor.getValue();
    assertEquals(2, event.interestCounts().size());

    // 데이터 상세 검증 (ID별로 매핑하여 값 확인)
    Map<UUID, InterestArticleCount> byId = event.interestCounts().stream()
        .collect(Collectors.toMap(InterestArticleCount::interestId, Function.identity()));

    assertEquals(1, byId.get(id1).articleCount());
    assertEquals("IT", byId.get(id1).interestName());
    assertEquals(2, byId.get(id2).articleCount());
    assertEquals("AUTO", byId.get(id2).interestName());
  }
}