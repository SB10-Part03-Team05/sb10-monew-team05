package com.codeit.monew.domain.useractivity.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.useractivity.entity.UserActivity;
import com.codeit.monew.domain.useractivity.entity.UserActivity.ArticleViewInfo;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

@ExtendWith(MockitoExtension.class)
class UserActivityEventListenerTest {

  @Mock
  private MongoTemplate mongoTemplate;

  @Mock
  private UserActivityMapper userActivityMapper;

  @Mock
  private CacheManager cacheManager;

  @Mock
  private Cache cache;

  @InjectMocks
  private UserActivityEventListener userActivityEventListener;

  @Captor
  private ArgumentCaptor<Query> queryCaptor;

  @Captor
  private ArgumentCaptor<UpdateDefinition> updateCaptor;

  @Test
  @DisplayName("사용자 회원가입 이벤트 수신 시 upsert 연산과 set 쿼리가 정확히 생성되어야 한다.")
  void should_upsert_and_set_when_user_registered() {
    // given
    UUID userId = UUID.randomUUID();
    UserRegisteredEvent event = new UserRegisteredEvent(
        userId,
        "test@email.com",
        "testNickname",
        Instant.now()
    );

    Query expectedQuery = new Query(Criteria.where("_id").is(userId.toString()));

    Update expectedUpdate = new Update()
        .set("email", event.email())
        .set("nickname", event.nickname())
        .set("createdAt", event.createdAt());
    given(cacheManager.getCache("userActivity")).willReturn(cache);
    // when
    userActivityEventListener.handleUserRegisteredEvent(event);

    // then
    verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(),
        eq(UserActivity.class));
    verify(cache).evict(userId);
    assertThat(queryCaptor.getValue()).isEqualTo(expectedQuery);
    assertThat(updateCaptor.getValue()).isEqualTo(expectedUpdate);
  }

  @Test
  @DisplayName("관심사 구독 이벤트 수신 시 upsert 연산과 push 쿼리가 정확히 생성되어야 한다.")
  void should_upsert_and_push_when_user_interest_subscribed() {
    // given
    UUID userId = UUID.randomUUID();
    InterestSubscribedEvent event = new InterestSubscribedEvent(
        userId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "관심사 이름",
        List.of(),
        0L,
        Instant.now()
    );
    UserActivity.SubscriptionInfo mockSubscriptionInfo = new UserActivity.SubscriptionInfo();

    Query expectedQuery = new Query(Criteria.where("_id").is(userId.toString()));

    Update expectedUpdate = new Update()
        .push("subscriptions")
        .atPosition(0)
        .slice(10)
        .each(mockSubscriptionInfo);

    given(userActivityMapper.toSubscriptionInfo(any())).willReturn(mockSubscriptionInfo);
    given(cacheManager.getCache("userActivity")).willReturn(cache);

    // when
    userActivityEventListener.handleInterestSubscribedEvent(event);

    // then
    verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(),
        eq(UserActivity.class));
    verify(cache).evict(userId);

    assertThat(queryCaptor.getValue()).isEqualTo(expectedQuery);
    assertThat(updateCaptor.getValue()).isEqualTo(expectedUpdate);
  }

  @Test
  @DisplayName("관심사 구독 취소 이벤트 수신 시 updateFirst 연산과 pull 쿼리가 정확히 생성되어야 한다.")
  void should_update_first_and_pull_when_user_interest_unsubscribed() {
    // given
    UUID userId = UUID.randomUUID();
    InterestUnSubscribedEvent event = new InterestUnSubscribedEvent(
        userId,
        UUID.randomUUID()
    );
    Query expectedQuery = new Query(Criteria.where("_id").is(userId.toString()));

    Update expectedUpdate = new Update().pull("subscriptions",
        new Document("interestId", event.interestId().toString())
    );
    given(cacheManager.getCache("userActivity")).willReturn(cache);
    // when
    userActivityEventListener.handleInterestUnSubscribedEvent(event);

    // then
    verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(),
        eq(UserActivity.class));
    verify(cache).evict(userId);

    assertThat(queryCaptor.getValue()).isEqualTo(expectedQuery);
    assertThat(updateCaptor.getValue()).isEqualTo(expectedUpdate);
  }

  @Test
  @DisplayName("관심사 삭제 이벤트 수신 시 대상 유저 조회, 전역 업데이트 및 다중 캐시 삭제가 정확히 수행되어야 한다.")
  void should_update_multi_and_evict_multiple_caches_when_interest_deleted() {
    // given
    UUID interestId = UUID.randomUUID();
    InterestDeletedEvent event = new InterestDeletedEvent(interestId);

    // 캐시 삭제 대상 유저 2명 설정
    UUID user1Id = UUID.randomUUID();
    UUID user2Id = UUID.randomUUID();

    UserActivity user1 = new UserActivity();
    user1.setId(user1Id.toString());
    UserActivity user2 = new UserActivity();
    user2.setId(user2Id.toString());

    Query expectedQuery = new Query(Criteria.where("subscriptions.interestId").is(interestId.toString()));
    expectedQuery.fields().include("_id");
    Update expectedUpdate = new Update().pull("subscriptions", new Document("interestId", interestId.toString()));

    given(mongoTemplate.find(any(Query.class), eq(UserActivity.class))).willReturn(List.of(user1, user2));
    given(cacheManager.getCache("userActivity")).willReturn(cache);

    // when
    userActivityEventListener.handleInterestDeletedEvent(event);

    // then
    verify(mongoTemplate).find(queryCaptor.capture(), eq(UserActivity.class));
    verify(mongoTemplate).updateMulti(any(Query.class), updateCaptor.capture(), eq(UserActivity.class));

    assertThat(queryCaptor.getValue()).isEqualTo(expectedQuery);
    assertThat(updateCaptor.getValue()).isEqualTo(expectedUpdate);

    verify(cache).evict(user1Id);
    verify(cache).evict(user2Id);
    verify(cache, times(2)).evict(any(UUID.class));
  }

  @Test
  @DisplayName("댓글 등록 이벤트 수신 시 upsert 연산과 push 쿼리가 정확히 생성되어야 한다.")
  void should_upsert_and_push_when_comment_created() {
    // given
    UUID userId = UUID.randomUUID();
    CommentCreatedEvent event = new CommentCreatedEvent(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "작성자",
        "댓글 본문",
        0L,
        Instant.now()
    );

    UserActivity.CommentInfo mockCommentInfo = new UserActivity.CommentInfo();
    mockCommentInfo.setContent("댓글 본문");

    Query expectedQuery = new Query(Criteria.where("_id").is(userId.toString()));

    Update expectedUpdate = new Update()
        .push("comments")
        .atPosition(0)
        .slice(10)
        .each(mockCommentInfo);

    given(userActivityMapper.toCommentInfo(any())).willReturn(mockCommentInfo);
    given(cacheManager.getCache("userActivity")).willReturn(cache);
    // when
    userActivityEventListener.handleCommentCreatedEvent(event);

    // then
    verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(),
        eq(UserActivity.class));
    verify(cache).evict(userId);
    assertThat(queryCaptor.getValue()).isEqualTo(expectedQuery);
    assertThat(updateCaptor.getValue()).isEqualTo(expectedUpdate);

  }

  @Test
  @DisplayName("댓글 수정 이벤트 수신 시 updateFirst 연산과 set 쿼리, updateMulti 연산과 set 쿼리가 정확히 생성되어야 한다.")
  void should_updateFirst_and_updateMulti_with_set_when_comment_updated() {
    // given
    UUID userId = UUID.randomUUID();
    CommentUpdatedEvent event = new CommentUpdatedEvent(
        userId,
        UUID.randomUUID(),
        "새로운 댓글 본문"
    );
    // 댓글 수정 쿼리
    Query expectedCommentQuery = new Query(
        Criteria.where("_id").is(event.userId().toString())
            .and("comments.id").is(event.commentId().toString())
    );
    Update expectedCommentUpdate = new Update().set("comments.$.content", event.newContent());

    // 댓글 좋아요에도 수정 사항 반영 쿼리
    Query expectedLikeQuery = new Query(
        Criteria.where("commentLikes.commentId").is(event.commentId().toString())
    );
    Update expectedLikeUpdate = new Update().set("commentLikes.$.commentContent",
        event.newContent());
    given(cacheManager.getCache("userActivity")).willReturn(cache);
    // when
    userActivityEventListener.handleCommentUpdatedEvent(event);

    // then
    verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(),
        eq(UserActivity.class));
    verify(mongoTemplate).updateMulti(queryCaptor.capture(), updateCaptor.capture(),
        eq(UserActivity.class));
    verify(cache).evict(userId);

    assertThat(queryCaptor.getAllValues().get(0)).isEqualTo(expectedCommentQuery);
    assertThat(updateCaptor.getAllValues().get(0)).isEqualTo(expectedCommentUpdate);

    assertThat(queryCaptor.getAllValues().get(1)).isEqualTo(expectedLikeQuery);
    assertThat(updateCaptor.getAllValues().get(1)).isEqualTo(expectedLikeUpdate);
  }

  @Test
  @DisplayName("댓글 좋아요 이벤트 수신 시 upsert 연산과 push 쿼리가 정확히 생성되어야 한다.")
  void should_upsert_and_push_when_comment_liked() {
    // given
    UUID userId = UUID.randomUUID();
    CommentLikedEvent event = new CommentLikedEvent(
        userId,
        UUID.randomUUID(),
        Instant.now(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "기사 제목",
        UUID.randomUUID(),
        "작성자",
        "댓글 본문",
        0L,
        Instant.now()
    );

    UserActivity.CommentLikeInfo mockCommentLikeInfo = new UserActivity.CommentLikeInfo();

    Query expectedQuery = new Query(Criteria.where("_id").is(userId.toString()));

    Update expectedUpdate = new Update()
        .push("commentLikes")
        .atPosition(0)
        .slice(10)
        .each(mockCommentLikeInfo);

    given(userActivityMapper.toCommentLikeInfo(any())).willReturn(mockCommentLikeInfo);
    given(cacheManager.getCache("userActivity")).willReturn(cache);
    // when
    userActivityEventListener.handleCommentLikedEvent(event);

    // then
    verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(),
        eq(UserActivity.class));
    verify(cache).evict(userId);

    assertThat(queryCaptor.getValue()).isEqualTo(expectedQuery);
    assertThat(updateCaptor.getValue()).isEqualTo(expectedUpdate);
  }

  @Test
  @DisplayName("댓글 좋아요 취소 이벤트 수신 시 updateFirst 연산과 pull 쿼리가 정확히 생성되어야 한다.")
  void should_update_first_and_pull_when_comment_liked_cancel() {
    // given
    UUID userId = UUID.randomUUID();
    CommentLikedCancelEvent event = new CommentLikedCancelEvent(
        userId,
        UUID.randomUUID()
    );

    Query expectedQuery = new Query(Criteria.where("_id").is(userId.toString()));

    Update expectedUpdate = new Update().pull("commentLikes",
        new Document("commentId", event.commentId().toString())
    );
    given(cacheManager.getCache("userActivity")).willReturn(cache);
    // when
    userActivityEventListener.handleCommentLikedCancelEvent(event);

    // then
    verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(),
        eq(UserActivity.class));
    verify(cache).evict(userId);

    assertThat(queryCaptor.getValue()).isEqualTo(expectedQuery);
    assertThat(updateCaptor.getValue()).isEqualTo(expectedUpdate);
  }

  @Test
  @DisplayName("기사 조회 이벤트 수신 시 updateFirst 연산과 pull 쿼리, upsert 연산과 push 쿼리가 정확히 생성되어야 한다.")
  void should_upsert_and_push_when_article_viewed() {
    // given
    UUID userId = UUID.randomUUID();
    ArticleViewedEvent event = new ArticleViewedEvent(
        UUID.randomUUID(),
        userId,
        Instant.now(),
        ArticleSource.NAVER,
        "fakeURL",
        "기사 제목",
        Instant.now(),
        "기사 요약",
        0L,
        0L
    );

    UserActivity.ArticleViewInfo mockArticleViewInfo = new ArticleViewInfo();

    Query expectedQuery = new Query(Criteria.where("_id").is(event.viewedBy().toString()));

    Update expectedPullUpdate = new Update().pull("articleViews",
        new Document("articleId", event.articleId().toString())
    );

    Update expectedPushUpdate = new Update()
        .push("articleViews")
        .atPosition(0)
        .slice(10)
        .each(mockArticleViewInfo);

    given(userActivityMapper.toArticleViewInfo(any())).willReturn(mockArticleViewInfo);
    given(cacheManager.getCache("userActivity")).willReturn(cache);
    // when
    userActivityEventListener.handleArticleViewedEvent(event);

    // then
    verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(),
        eq(UserActivity.class));
    verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(),
        eq(UserActivity.class));
    verify(cache).evict(event.viewedBy());

    assertThat(queryCaptor.getAllValues().get(0)).isEqualTo(expectedQuery);
    assertThat(updateCaptor.getAllValues().get(0)).isEqualTo(expectedPullUpdate);
    assertThat(queryCaptor.getAllValues().get(1)).isEqualTo(expectedQuery);
    assertThat(updateCaptor.getAllValues().get(1)).isEqualTo(expectedPushUpdate);
  }
}