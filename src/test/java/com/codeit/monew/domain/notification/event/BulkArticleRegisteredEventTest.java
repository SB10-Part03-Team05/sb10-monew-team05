package com.codeit.monew.domain.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.monew.global.exception.common.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BulkArticleRegisteredEventTest {
  @Test
  @DisplayName("BulkArticleRegisteredEvent 생성 시 interestCounts가 null이면 빈 리스트로 초기화된다.")
  void constructor_null_list_to_empty() {
    // given & when
    BulkArticleRegisteredEvent event = new BulkArticleRegisteredEvent(null);

    // then
    assertThat(event.interestCounts()).isNotNull().isEmpty();
  }

  @Test
  @DisplayName("BulkArticleRegisteredEvent는 방어적 복사를 통해 원본 리스트의 변경으로부터 안전하다.")
  void defensive_copy_test() {
    // given
    List<BulkArticleRegisteredEvent.InterestArticleCount> mutableList = new ArrayList<>();
    mutableList.add(new BulkArticleRegisteredEvent.InterestArticleCount(UUID.randomUUID(), "테스트", 1));
    BulkArticleRegisteredEvent event = new BulkArticleRegisteredEvent(mutableList);

    // when
    mutableList.clear(); // 원본 리스트 변경

    // then
    assertThat(event.interestCounts()).hasSize(1); // 이벤트 내부 리스트는 유지되어야 함
  }

  @Test
  @DisplayName("InterestArticleCount 생성 시 interestId가 null이면 InvalidParameterException이 발생한다.")
  void validation_null_id() {
    // given, when & then
    assertThatThrownBy(() -> new BulkArticleRegisteredEvent.InterestArticleCount(null, "테스트", 1))
        .isInstanceOf(InvalidParameterException.class);
  }

  @Test
  @DisplayName("InterestArticleCount 생성 시 기사 수가 음수이면 IllegalArgumentException이 발생한다.")
  void validation_negative_count() {
    // given, when & then
    assertThatThrownBy(() -> new BulkArticleRegisteredEvent.InterestArticleCount(UUID.randomUUID(), "테스트", -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("기사 수는 0보다 작을 수 없습니다.");
  }
}