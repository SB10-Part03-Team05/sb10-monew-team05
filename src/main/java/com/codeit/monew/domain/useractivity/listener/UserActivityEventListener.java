package com.codeit.monew.domain.useractivity.listener;

import com.codeit.monew.domain.useractivity.entity.UserActivity;
import com.codeit.monew.domain.useractivity.repository.UserActivityRepository;
import com.codeit.monew.global.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityEventListener {

  private final UserActivityRepository userActivityRepository;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleUserRegisteredEvent(UserRegisteredEvent event) {
    log.debug("[USER_ACTIVITY] 새로운 유저 도큐먼트 생성 시작: {}", event.userId());

    UserActivity userActivity = new UserActivity();
    userActivity.setId(event.userId().toString());
    userActivity.setEmail(event.email());
    userActivity.setNickname(event.nickname());
    userActivity.setCreatedAt(event.createdAt());

    userActivityRepository.save(userActivity);

    log.info("[USER_ACTIVITY] 새로운 유저 도큐먼트 생성 완료");
  }

  //todo handleInterestSubscribedEvent() 관심사 등록 이벤트

  //todo handleCommentCreatedEvent() 댓글 등록 이벤트

  //todo handleCommentLikedEvent() 댓글 좋아요 등록 이벤트

  //todo handleArticleViewedEvent() 기사 조회 갱신 이벤트


}
