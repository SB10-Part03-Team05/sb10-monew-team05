package com.codeit.monew.domain.interest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.interest.dto.request.InterestRegisterRequest;
import com.codeit.monew.domain.interest.dto.request.InterestUpdateRequest;
import com.codeit.monew.domain.interest.dto.response.CursorPageResponseInterestDto;
import com.codeit.monew.domain.interest.dto.response.InterestDto;
import com.codeit.monew.domain.interest.dto.response.SubscriptionDto;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.interest.repository.KeywordRepository;
import com.codeit.monew.domain.interest.repository.SubscriptionRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.exception.Interest.DuplicateInterestException;
import com.codeit.monew.global.exception.Interest.InterestNotFoundException;
import com.codeit.monew.global.exception.Interest.SubscriptionNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

  @Mock
  private InterestRepository interestRepository;
  @Mock
  private KeywordRepository keywordRepository;
  @Mock
  private SubscriptionRepository subscriptionRepository;
  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private InterestService interestService;

  private Interest createInterest(UUID interestId, String name) {
    Interest interest = Interest.create(name);
    ReflectionTestUtils.setField(interest, "id", interestId);
    return interest;
  }

  private User createUser(UUID userId) {
    User user = new User("test@email.com", "testNickname", "testPassword");
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }

  // 1. 관심사 등록
  @Nested
  @DisplayName("관심사 등록 테스트")
  class register {

    @Test
    @DisplayName("관심사를 등록할 수 있다.")
    void success_register_interest() {
      // given
      InterestRegisterRequest request = new InterestRegisterRequest("스포츠", List.of("축구", "야구"));
      given(interestRepository.findAllNamesWithLock()).willReturn(List.of());

      Interest savedInterest = createInterest(UUID.randomUUID(), "스포츠");
      given(interestRepository.save(any())).willReturn(savedInterest);

      // when
      InterestDto result = interestService.register(request);

      // then
      assertNotNull(result);
      assertEquals("스포츠", result.name());
      verify(interestRepository).save(any());
      verify(keywordRepository).saveAll(any());
    }

    @Test
    @DisplayName("80% 이상 유사한 관심사가 있으면 등록에 실패한다.")
    void fail_register_interest_when_similar_name_exists() {
      // given
      InterestRegisterRequest request = new InterestRegisterRequest("해외 스포츠", List.of("축구"));
      given(interestRepository.findAllNamesWithLock()).willReturn(List.of("해외 스포츠츠"));

      // when, then
      assertThrows(DuplicateInterestException.class,
          () -> interestService.register(request));
      verify(interestRepository, never()).save(any());
    }
  }

  // 2. 관심사 수정
  @Nested
  @DisplayName("관심사 수정 테스트")
  class update {

    @Test
    @DisplayName("관심사 키워드를 수정할 수 있다.")
    void success_update_interest() {
      // given
      UUID interestId = UUID.randomUUID();
      InterestUpdateRequest request = new InterestUpdateRequest(List.of("농구", "배구"));
      Interest interest = createInterest(interestId, "스포츠");

      given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));

      // when
      InterestDto result = interestService.update(interestId, request);

      // then
      assertNotNull(result);
      verify(keywordRepository).deleteAllByInterestId(interestId);
      verify(keywordRepository).saveAll(any());
    }

    @Test
    @DisplayName("존재하지 않는 관심사를 수정하면 실패한다.")
    void fail_update_interest_when_not_found() {
      // given
      UUID interestId = UUID.randomUUID();
      InterestUpdateRequest request = new InterestUpdateRequest(List.of("농구"));

      given(interestRepository.findById(interestId)).willReturn(Optional.empty());

      // when, then
      assertThrows(InterestNotFoundException.class,
          () -> interestService.update(interestId, request));
    }
  }

  // 3. 관심사 삭제
  @Nested
  @DisplayName("관심사 삭제 테스트")
  class delete {

    @Test
    @DisplayName("관심사를 삭제할 수 있다.")
    void success_delete_interest() {
      // given
      UUID interestId = UUID.randomUUID();
      Interest interest = createInterest(interestId, "스포츠");

      given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));

      // when
      interestService.delete(interestId);

      // then
      verify(interestRepository).delete(interest);
    }

    @Test
    @DisplayName("존재하지 않는 관심사를 삭제하면 실패한다.")
    void fail_delete_interest_when_not_found() {
      // given
      UUID interestId = UUID.randomUUID();

      given(interestRepository.findById(interestId)).willReturn(Optional.empty());

      // when, then
      assertThrows(InterestNotFoundException.class,
          () -> interestService.delete(interestId));
    }
  }

  // 4. 관심사 목록 조회
  @Nested
  @DisplayName("관심사 목록 조회 테스트")
  class getList {

    @Test
    @DisplayName("관심사 목록을 조회할 수 있다.")
    void success_get_interest_list() {
      // given
      UUID userId = UUID.randomUUID();
      UUID interestId = UUID.randomUUID();

      InterestDto interestDto = new InterestDto(
          interestId, "스포츠", List.of("축구", "야구"), 10L, false
      );

      CursorPageResponseInterestDto expected = new CursorPageResponseInterestDto(
          List.of(interestDto),
          null,
          null,
          1,
          1L,
          false
      );

      given(interestRepository.findInterests(
          null, "name", "ASC", null, null, 10, userId
      )).willReturn(expected);

      // when
      CursorPageResponseInterestDto result = interestService.getList(
          null, "name", "ASC", null, null, 10, userId
      );

      // then
      assertNotNull(result);
      assertEquals(1, result.content().size());
      assertEquals("스포츠", result.content().get(0).name());
      assertFalse(result.hasNext());
      verify(interestRepository).findInterests(
          null, "name", "ASC", null, null, 10, userId
      );
    }

    @Test
    @DisplayName("검색어로 관심사 목록을 조회할 수 있다.")
    void success_get_interest_list_with_keyword() {
      // given
      UUID userId = UUID.randomUUID();

      CursorPageResponseInterestDto expected = new CursorPageResponseInterestDto(
          List.of(),
          null,
          null,
          0,
          0L,
          false
      );

      given(interestRepository.findInterests(
          "없는검색어", "name", "ASC", null, null, 10, userId
      )).willReturn(expected);

      // when
      CursorPageResponseInterestDto result = interestService.getList(
          "없는검색어", "name", "ASC", null, null, 10, userId
      );

      // then
      assertNotNull(result);
      assertEquals(0, result.content().size());
      assertFalse(result.hasNext());
    }
  }

  // 5. 관심사 구독
  @Nested
  @DisplayName("관심사 구독 테스트")
  class subscribe {
    @Test
    @DisplayName("관심사를 구독할 수 있다.")
    void success_subscribe_interest() {
      // given
      UUID interestId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      Interest interest = createInterest(interestId, "스포츠");
      User user = new User("test@email.com", "testNickname", "testPassword");
      ReflectionTestUtils.setField(user, "id", userId);

      given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(subscriptionRepository.save(any())).willAnswer(i -> i.getArgument(0));

      // when
      SubscriptionDto result = interestService.subscribe(interestId, userId);

      // then
      assertNotNull(result);
      assertEquals(interestId, result.interestId());
      verify(subscriptionRepository).save(any());
      verify(interestRepository).incrementSubscriberCount(interestId);
    }

    @Test
    @DisplayName("존재하지 않는 관심사를 구독하면 실패한다.")
    void fail_subscribe_when_interest_not_found() {
      // given
      UUID interestId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      given(interestRepository.findById(interestId)).willReturn(Optional.empty());

      // when, then
      assertThrows(InterestNotFoundException.class,
          () -> interestService.subscribe(interestId, userId));

      verify(subscriptionRepository, never()).save(any());
    }
  }

  // 6. 관심사 구독 취소
  @Nested
  @DisplayName("관심사 구독 취소 테스트")
  class unsubscribe {
    @Test
    @DisplayName("관심사 구독을 취소할 수 있다.")
    void success_unsubscribe_interest() {
      // given
      UUID interestId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      Interest interest = createInterest(interestId, "스포츠");

      given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
      given(subscriptionRepository.deleteByUserIdAndInterestId(userId, interestId)).willReturn(1L);

      // when
      interestService.unsubscribe(interestId, userId);

      // then
      verify(subscriptionRepository).deleteByUserIdAndInterestId(userId, interestId);
      verify(interestRepository).decrementSubscriberCount(interestId);
    }

    @Test
    @DisplayName("존재하지 않는 관심사를 구독 취소하면 실패한다.")
    void fail_unsubscribe_when_interest_not_found() {
      // given
      UUID interestId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      given(interestRepository.findById(interestId)).willReturn(Optional.empty());

      // when, then
      assertThrows(InterestNotFoundException.class,
          () -> interestService.unsubscribe(interestId, userId));

      verify(subscriptionRepository, never()).deleteByUserIdAndInterestId(any(), any());
    }

    @Test
    @DisplayName("구독 중이지 않은 구독 취소하면 실패한다.")
    void fail_unsubscribe_when_not_subscribed() {
      // given
      UUID interestId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      Interest interest = createInterest(interestId, "스포츠");

      given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
      given(subscriptionRepository.deleteByUserIdAndInterestId(userId, interestId)).willReturn(0L);

      // when, then
      assertThrows(SubscriptionNotFoundException.class,
          () -> interestService.unsubscribe(interestId, userId));

      verify(interestRepository, never()).decrementSubscriberCount(any());
    }

  }

}
