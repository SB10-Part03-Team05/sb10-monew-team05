package com.codeit.monew.domain.useractivity.listener;

import com.codeit.monew.domain.useractivity.entity.UserActivity;
import com.codeit.monew.domain.useractivity.entity.UserActivity.ArticleViewInfo;
import com.codeit.monew.domain.useractivity.entity.UserActivity.CommentInfo;
import com.codeit.monew.domain.useractivity.entity.UserActivity.CommentLikeInfo;
import com.codeit.monew.domain.useractivity.entity.UserActivity.SubscriptionInfo;
import com.codeit.monew.domain.useractivity.event.ArticleViewedEvent;
import com.codeit.monew.domain.useractivity.event.CommentCreatedEvent;
import com.codeit.monew.domain.useractivity.event.CommentLikedCancelEvent;
import com.codeit.monew.domain.useractivity.event.CommentLikedEvent;
import com.codeit.monew.domain.useractivity.event.CommentUpdatedEvent;
import com.codeit.monew.domain.useractivity.event.InterestDeletedEvent;
import com.codeit.monew.domain.useractivity.event.InterestSubscribedEvent;
import com.codeit.monew.domain.useractivity.event.InterestUnSubscribedEvent;
import com.codeit.monew.domain.useractivity.event.UserRegisteredEvent;
import com.codeit.monew.domain.useractivity.mapper.UserActivityMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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

  private final MongoTemplate mongoTemplate;
  private final UserActivityMapper userActivityMapper;
  private final CacheManager cacheManager;

  // DB 업데이트 이후 이전 캐시를 삭제하는 헬퍼 메서드
  private void evictUserActivityCache(UUID userId) {
    Cache cache = cacheManager.getCache("userActivity");
    if (cache == null) {
      log.warn("[USER_ACTIVITY_CACHE] 캐시를 찾지 못해 삭제 스킵: userId={}", userId);
      return;
    }
    cache.evict(userId); // @Cacheable의 key가 UUID이므로 그대로 전달
    log.debug("[USER_ACTIVITY_CACHE] 캐시 안전 삭제 완료: userId={}", userId);
  }

  // 댓글 수정 시 연관된 타인들의 ID를 찾아서 캐시를 지우는 헬퍼 메서드
  private void queryForLikedUsers(UUID commentId) {
    Query query = new Query(Criteria.where("commentLikes.commentId").is(commentId.toString()));
    query.fields().include("_id");

    List<UserActivity> likedUsers = mongoTemplate.find(query, UserActivity.class);

    for (UserActivity user : likedUsers) {
      try {
        // 찾아낸 모든 타인의 캐시를 지워서 다음 조회 시 최신 데이터를 보게 함
        evictUserActivityCache(UUID.fromString(user.getId()));
      } catch (IllegalArgumentException e) {
        log.warn("[USER_ACTIVITY_CACHE] 잘못된 userId 형식으로 캐시 삭제 스킵: id={}", user.getId());
      }
    }
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleUserRegisteredEvent(UserRegisteredEvent event) {
    log.debug("[USER_ACTIVITY] 새로운 유저 도큐먼트 생성 시작: userId={}", event.userId());

    Query query = new Query(Criteria.where("_id").is(event.userId().toString()));

    Update update = new Update()
        .set("email", event.email())
        .set("nickname", event.nickname())
        .set("createdAt", event.createdAt());

    mongoTemplate.upsert(query, update, UserActivity.class);

    evictUserActivityCache(event.userId());
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
        .atPosition(0)
        .slice(10)
        .each(newSubscriptionInfo);

    mongoTemplate.upsert(query, update, UserActivity.class);

    evictUserActivityCache(event.userId());
    log.info("[USER_ACTIVITY] 관심사 구독 목록 업데이트 완료");
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleInterestUnSubscribedEvent(InterestUnSubscribedEvent event) {
    log.debug("[USER_ACTIVITY] 관심사 구독 취소 이벤트 수신: userId={}, interestId={}", event.userId(),
        event.interestId());

    Query query = new Query(Criteria.where("_id").is(event.userId().toString()));

    Update pullUpdate = new Update().pull("subscriptions",
        new Document("interestId", event.interestId().toString())
    );

    mongoTemplate.updateFirst(query, pullUpdate, UserActivity.class);

    evictUserActivityCache(event.userId());
    log.info("[USER_ACTIVITY] 관심사 구독 취소 완료");
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleInterestDeletedEvent(InterestDeletedEvent event) {
    log.debug("[USER_ACTIVITY] 관심사 전역 삭제 반영 시작: interestId={}", event.interestId().toString());

    // DB에서 제거하기 전 해당 관심사를 가진 유저들을 검색
    Query findQuery = new Query(Criteria.where("subscriptions.interestId").is(
        event.interestId().toString()));
    findQuery.fields().include("_id");
    List<UserActivity> usersToEvict = mongoTemplate.find(findQuery, UserActivity.class);

    // 모든 유저의 subscriptions 배열에서 해당 관심사 제거
    Update pullUpdate = new Update().pull("subscriptions", new Document("interestId",
        event.interestId().toString()));
    mongoTemplate.updateMulti(findQuery, pullUpdate, UserActivity.class);

    // 미리 확보한 유저 리스트를 바탕으로 캐시 무효화 진행
    for (UserActivity user : usersToEvict) {
      try {
        evictUserActivityCache(UUID.fromString(user.getId()));
      } catch (Exception e) {
        log.warn("[USER_ACTIVITY_CACHE] 전역 삭제에 따른 캐시 무효화 실패: userId={}", user.getId());
      }
    }
    log.info("[USER_ACTIVITY] 전역 관심사 삭제 반영 완료");
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentCreatedEvent(CommentCreatedEvent event) {
    log.debug("[USER_ACTIVITY] 댓글 등록 이벤트 수신: userId={}, commentId={}", event.userId(),
        event.commentId());

    Query query = new Query(Criteria.where("_id").is(event.userId().toString()));

    CommentInfo newCommentInfo = userActivityMapper.toCommentInfo(event);

    Update update = new Update()
        .push("comments")
        .atPosition(0)
        .slice(10)
        .each(newCommentInfo);

    mongoTemplate.upsert(query, update, UserActivity.class);

    evictUserActivityCache(event.userId());
    log.info("[USER_ACTIVITY] 댓글 목록 업데이트 완료");
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentUpdatedEvent(CommentUpdatedEvent event) {
    log.debug("[USER_ACTIVITY] 댓글 수정 이벤트 수신: userId={}, commentId={}",
        event.userId(), event.commentId());

    Query commentQuery = new Query(
        Criteria.where("_id").is(event.userId().toString())
            .and("comments.id").is(event.commentId().toString())
    );
    // 댓글 수정
    Update commentUpdate = new Update().set("comments.$.content", event.newContent());
    mongoTemplate.updateFirst(commentQuery, commentUpdate, UserActivity.class);
    // 작성자 본인 캐시 삭제
    evictUserActivityCache(event.userId());

    // 댓글 좋아요 활동 내역에도 모두 반영
    Query likeQuery = new Query(
        Criteria.where("commentLikes.commentId").is(event.commentId().toString()));
    Update likeUpdate = new Update().set("commentLikes.$.commentContent", event.newContent());

    try {
      mongoTemplate.updateMulti(likeQuery, likeUpdate, UserActivity.class);
    } finally {
      // 실패 경로에서도 보수적으로 무효화
      queryForLikedUsers(event.commentId());
    }

    log.info("[USER_ACTIVITY] 댓글 내용 수정 완료");
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentLikedEvent(CommentLikedEvent event) {
    log.debug("[USER_ACTIVITY] 댓글 좋아요 이벤트 수신: userId={}, commentLikeId={}", event.userId(),
        event.commentLikeId());

    Query query = new Query(Criteria.where("_id").is(event.userId().toString()));

    CommentLikeInfo newCommentLikeInfo = userActivityMapper.toCommentLikeInfo(event);

    Update update = new Update()
        .push("commentLikes")
        .atPosition(0)
        .slice(10)
        .each(newCommentLikeInfo);

    mongoTemplate.upsert(query, update, UserActivity.class);

    evictUserActivityCache(event.userId());
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

    evictUserActivityCache(event.userId());
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

    // 새로운 기사 조회 이력을 배열 맨 앞에 추가
    ArticleViewInfo newArticleViewInfo = userActivityMapper.toArticleViewInfo(event);

    Update pushUpdate = new Update()
        .push("articleViews")
        .atPosition(0)
        .slice(10)
        .each(newArticleViewInfo);

    mongoTemplate.upsert(query, pushUpdate, UserActivity.class);
    evictUserActivityCache(event.viewedBy());
    log.info("[USER_ACTIVITY] 기사 조회 목록 업데이트 완료");
  }

}
