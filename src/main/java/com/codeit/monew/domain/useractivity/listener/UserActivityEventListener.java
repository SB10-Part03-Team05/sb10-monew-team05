package com.codeit.monew.domain.useractivity.listener;

import com.codeit.monew.domain.useractivity.entity.UserActivity;
import com.codeit.monew.domain.useractivity.entity.UserActivity.ArticleViewInfo;
import com.codeit.monew.domain.useractivity.entity.UserActivity.CommentInfo;
import com.codeit.monew.domain.useractivity.entity.UserActivity.CommentLikeInfo;
import com.codeit.monew.domain.useractivity.entity.UserActivity.SubscriptionInfo;
import com.codeit.monew.domain.useractivity.mapper.UserActivityMapper;
import com.codeit.monew.domain.useractivity.repository.UserActivityRepository;
import com.codeit.monew.global.event.ArticleViewedEvent;
import com.codeit.monew.global.event.CommentCreatedEvent;
import com.codeit.monew.global.event.CommentDeletedEvent;
import com.codeit.monew.global.event.CommentLikedCancelEvent;
import com.codeit.monew.global.event.CommentLikedEvent;
import com.codeit.monew.global.event.CommentUpdatedEvent;
import com.codeit.monew.global.event.InterestSubscribedEvent;
import com.codeit.monew.global.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
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

  // todo 관심사 구독 취소 이벤트

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

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentUpdatedEvent(CommentUpdatedEvent event) {
    log.debug("[USER_ACTIVITY] 댓글 수정 이벤트 수신: userId={}, commentId={}",
        event.userId(), event.commentId());

    Query query = new Query(
        Criteria.where("_id").is(event.userId().toString())
            .and("comments._id").is(event.commentId().toString())
    );

    Update update = new Update().set("comments.$.content", event.newContent());

    mongoTemplate.updateFirst(query, update, UserActivity.class);

    log.info("[USER_ACTIVITY] 댓글 내용 수정 완료");
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentDeletedEvent(CommentDeletedEvent event) {
    log.debug("[USER_ACTIVITY] 댓글 삭제 이벤트 수신:  commentId={}", event.commentId());

    Query query = new Query();

    // 댓글과 연관된 댓글 좋아요를 활동 내역에서 제거
    Update pullUpdate = new Update()
        .pull("comments", new Document("_id", event.commentId().toString()))
        .pull("commentLikes", new Document("commentId", event.commentId().toString()));

    mongoTemplate.updateMulti(query, pullUpdate, UserActivity.class);

    log.info("[USER_ACTIVITY] 댓글 내용 삭제 완료");
  }


  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentLikedEvent(CommentLikedEvent event) {
    log.debug("[USER_ACTIVITY] 댓글 좋아요 이벤트 수신: userId={}, commentLikeId={}", event.commentUserId(),
        event.commentLikeId());

    Query query = new Query(Criteria.where("_id").is(event.commentUserId().toString()));

    CommentLikeInfo newCommentLikeInfo = userActivityMapper.toCommentLikeInfo(event);

    Update update = new Update()
        .push("commentLikes")
        .slice(-10)
        .each(newCommentLikeInfo);

    mongoTemplate.updateFirst(query, update, UserActivity.class);

    log.info("[USER_ACTIVITY] 댓글 좋아요 목록 업데이트 완료");
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentLikedCancelEvent(CommentLikedCancelEvent event) {
    log.debug("[USER_ACTIVITY] 댓글 좋아요 취소 이벤트 수신: userId={}", event.userId());

    Query query = new Query(Criteria.where("_id").is(event.userId().toString()));

    Update pullUpdate = new Update().pull("commentLikes",
        new Document("commentId", event.commentId().toString())
    );

    mongoTemplate.updateFirst(query, pullUpdate, UserActivity.class);

    log.info("[USER_ACTIVITY] 댓글 좋아요 취소 완료");
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleArticleViewedEvent(ArticleViewedEvent event) {
    log.debug("[USER_ACTIVITY] 기사 조회 이벤트 수신: userId={}, articleId={}", event.viewedBy(),
        event.articleId());

    Query query = new Query(Criteria.where("_id").is(event.viewedBy().toString()));

    // 기존 배열에 동일한 기사가 있다면 제거
    Update pullUpdate = new Update().pull("articleViews",
        new Document("articleId", event.articleId().toString())
    );
    mongoTemplate.updateFirst(query, pullUpdate, UserActivity.class);

    // 새로운 기사 조회 이력을 배열 맨 뒤에 추가
    ArticleViewInfo newArticleViewInfo = userActivityMapper.toArticleViewInfo(event);

    Update pushUpdate = new Update()
        .push("articleViews")
        .slice(-10)
        .each(newArticleViewInfo);

    mongoTemplate.updateFirst(query, pushUpdate, UserActivity.class);

    log.info("[USER_ACTIVITY] 기사 조회 목록 업데이트 완료");
  }


}
