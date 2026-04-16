package com.codeit.monew.domain.article.entity;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.global.common.base.BaseUpdatableEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
    name = "articles",
    indexes = {
        @Index(name = "idx_articles_active_publish_date", columnList = "publish_date DESC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@SQLDelete(sql = "UPDATE articles SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL") // 조회 시 deleted_at이 null인 데이터만 가져오도록 글로벌 필터링
public class Article extends BaseUpdatableEntity {

  // 기본 5개 필드 (출처, 링크, 제목, 날짜, 요약)
  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false, length = 50)
  private ArticleSource source; // 출처

  @Column(name = "source_url", nullable = false, unique = true, length = 2048)
  private String sourceUrl; // 원본 기사 링크

  @Column(name = "title", nullable = false, length = 200)
  private String title; // 제목

  @Column(name = "publish_date", nullable = false)
  private Instant publishDate; // 날짜

  @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
  private String summary; // 요약

  // 2. One-To-Many 매핑 (댓글, 관심사 중간 엔티티)
  @Builder.Default
  @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Comment> comments = new ArrayList<>(); // 댓글

  @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ArticleInterest> articleInterests = new ArrayList<>(); // 관심사

  // 3. 조회수 (article_view_histories 테이블 개수 기반)
  @Basic(fetch = FetchType.LAZY)
  @Formula("(SELECT COUNT(*) FROM article_view_histories v WHERE v.article_id = id)")
  private Long viewCount = 0L; // 조회수

  // 메타 데이터
  @Column(name = "deleted_at")
  private Instant deletedAt;

  // 엔티티 무결성 검증 메서드
  private static void validateArticle(ArticleSource source, String sourceUrl, String title,
      Instant publishDate, String summary) {
    // 필수 값 검증
    if (source == null || isTextEmpty(sourceUrl) || isTextEmpty(title) ||
        publishDate == null || isTextEmpty(summary)) {
      throw new IllegalArgumentException();
      // todo: 커스텀 예외로 변경하기
      //throw new BusinessException(ErrorCode.INVALID_ARTICLE_FORMAT);
    }

    // 길이 제한 검증
    if (title.length() > 200) {
      throw new IllegalArgumentException();
      // todo: 커스텀 예외로 변경하기
      //throw new BusinessException(ErrorCode.ARTICLE_TITLE_TOO_LONG);
    }
    if (sourceUrl.length() > 2048) {
      throw new IllegalArgumentException();
      // todo: 커스텀 예외로 변경하기
      //throw new BusinessException(ErrorCode.ARTICLE_URL_TOO_LONG);
    }
  }

  private static boolean isTextEmpty(String text) {
    return text == null || text.isBlank();
  }


  // 정적 팩토리 메서드
  public static Article createArticle(ArticleSource source, String sourceUrl, String title,
      Instant publishDate, String summary) {
    validateArticle(source, sourceUrl, title, publishDate, summary);
    return Article.builder()
        .source(source)
        .sourceUrl(sourceUrl)
        .title(title)
        .publishDate(publishDate)
        .summary(summary)
        .build();
  }

  // 업데이트 메서드
  public void update(ArticleSource source, String sourceUrl, String title, Instant publishDate,
      String summary) {
    validateArticle(source, sourceUrl, title, publishDate, summary);
    this.source = source;
    this.sourceUrl = sourceUrl;
    this.title = title;
    this.publishDate = publishDate;
    this.summary = summary;
  }

  // 연관관계 편의 메서드
  public void addComment(Comment comment) {
    if (this.comments.contains(comment) || comment == null) {
      return;
    }

    this.comments.add(comment);

//    if (comment.getArticle() != this) {
//      comment.setArticle(this);
//    }
  }

  public void addInterest(Interest interest) {
    if (interest == null) {
      return;
    }

    // 이미 등록된 관심사인지 UUID 기반으로 비교하여 중복 방지
    boolean alreadyExists = this.articleInterests.stream()
        .anyMatch(articleInterest -> {
          Interest existingInterest = articleInterest.getInterest();
          // 둘 중 하나라도 ID가 없으면(신규 엔티티면) 참조 동일성(==)으로 비교
          if (existingInterest.getId() == null || interest.getId() == null) {
            return existingInterest == interest;
          }
          // ID가 있다면 UUID 값으로 비교
          return existingInterest.getId().equals(interest.getId());
        });

    if (!alreadyExists) {
      ArticleInterest mapping = ArticleInterest.create(this, interest);
      this.articleInterests.add(mapping);
    }
  }
}
