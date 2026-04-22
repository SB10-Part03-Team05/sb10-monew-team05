package com.codeit.monew.domain.useractivity.entity;

import com.codeit.monew.domain.article.ArticleSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Getter
@NoArgsConstructor
@Document(collection = "user-activities")
public class UserActivity {

  @Id
  @Setter
  private String id;
  @Setter
  private String email;
  @Setter
  private String nickname;
  @Setter
  private Instant createdAt;
  // 구독 중인 관심사
  private List<SubscriptionInfo> subscriptions = new ArrayList<>();

  // 최근 작성한 댓글 (최대 10건)
  private List<CommentInfo> comments = new ArrayList<>();

  // 최근 좋아요 누른 댓글 (최대 10건)
  private List<CommentLikeInfo> commentLikes = new ArrayList<>();

  // 최근 본 뉴스 기사 (최대 10건)
  private List<ArticleViewInfo> articleViews = new ArrayList<>();

  public void addComment(CommentInfo comment) {
    this.comments.add(0, comment);
    if (this.comments.size() > 10) {
      this.comments.remove(this.comments.size() - 1);
    }
  }

  public void addCommentLike(CommentLikeInfo commentLike) {
    this.commentLikes.add(0, commentLike);
    if (this.commentLikes.size() > 10) {
      this.commentLikes.remove(this.commentLikes.size() - 1);
    }
  }

  public void addArticleView(ArticleViewInfo articleView) {
    this.articleViews.add(0, articleView);
    if (this.articleViews.size() > 10) {
      this.articleViews.remove(this.articleViews.size() - 1);
    }
  }

  public void addSubscription(SubscriptionInfo subscription) {
    this.subscriptions.add(0, subscription);
  }

  @Getter
  @Setter
  public static class SubscriptionInfo {

    private String id;
    private String interestId;
    private String interestName;
    private List<String> interestKeywords = new ArrayList<>();
    private Long interestSubscriberCount;
    private Instant createdAt;
  }

  @Getter
  @Setter
  public static class CommentInfo {

    private String id;
    private String articleId;
    private String articleTitle;
    private String userId;
    private String userNickname;
    private String content;
    private Long likeCount;
    private Instant createdAt;
  }

  @Getter
  @Setter
  public static class CommentLikeInfo {

    private String id;
    private Instant createdAt;
    private String commentId;
    private String articleId;
    private String articleTitle;
    private String commentUserId;
    private String commentUserNickname;
    private String commentContent;
    private Long commentLikeCount;
    private Instant commentCreatedAt;
  }

  @Getter
  @Setter
  public static class ArticleViewInfo {

    private String id;
    private String viewedBy;
    private Instant createdAt;
    private String articleId;
    @Field(targetType = FieldType.STRING)
    private ArticleSource source;
    private String sourceUrl;
    private String articleTitle;
    private Instant articlePublishedDate;
    private String articleSummary;
    private Long articleCommentCount;
    private Long articleViewCount;
  }
}
