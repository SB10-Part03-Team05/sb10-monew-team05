package com.codeit.monew.domain.useractivity.listener;

import com.codeit.monew.domain.useractivity.entity.UserActivity;
import com.codeit.monew.domain.useractivity.entity.UserActivity.CommentInfo;
import com.codeit.monew.domain.useractivity.entity.UserActivity.SubscriptionInfo;
import com.codeit.monew.domain.useractivity.mapper.UserActivityMapper;
import com.codeit.monew.domain.useractivity.repository.UserActivityRepository;
import com.codeit.monew.global.event.CommentCreatedEvent;
import com.codeit.monew.global.event.InterestSubscribedEvent;
import com.codeit.monew.global.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityEventListener {

  private final UserActivityRepository userActivityRepository;
  private final MongoTemplate mongoTemplate;
  private final UserActivityMapper userActivityMapper;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleUserRegisteredEvent(UserRegisteredEvent event) {
    log.debug("[USER_ACTIVITY] 새로운 유저 도큐먼트 생성 시작: userId={}", event.userId());

    UserActivity userActivity = userActivityMapper.toUserActivity(event);

    userActivityRepository.save(userActivity);

    log.info("[USER_ACTIVITY] 새로운 유저 도큐먼트 생성 완료");
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleInterestSubscribedEvent(InterestSubscribedEvent event) {
    log.debug("[USER_ACTIVITY] 관심사 구독 이벤트 수신: userId={}, interestId={}", event.userId(),
        event.interestId());

    Query query = new Query(Criteria.where("_id").is(event.userId().toString()));

    SubscriptionInfo newSubscriptionInfo = userActivityMapper.toSubscriptionInfo(event);

    Update update = new Update()
        .push("subscriptions")
        .each(newSubscriptionInfo);

    mongoTemplate.updateFirst(query, update, UserActivity.class);

    log.info("[USER_ACTIVITY] 관심사 구독 목록 업데이트 완료");
  }

  //todo handleCommentCreatedEvent() 댓글 등록 이벤트
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentCreatedEvent(CommentCreatedEvent event) {
    log.debug("[USER_ACTIVITY] 댓글 등록 이벤트 수신: userId={}, commentId={}", event.userId(),
        event.commentId());

    Query query = new Query(Criteria.where("_id").is(event.userId().toString()));

    CommentInfo newCommentInfo = userActivityMapper.toCommentInfo(event);

    Update update = new Update()
        .push("comments")
        .slice(-10)
        .each(newCommentInfo);

    mongoTemplate.updateFirst(query, update, UserActivity.class);

    log.info("[USER_ACTIVITY] 댓글 목록 업데이트 완료");
  }

  //todo handleCommentLikedEvent() 댓글 좋아요 등록 이벤트

  //todo handleArticleViewedEvent() 기사 조회 갱신 이벤트


}
