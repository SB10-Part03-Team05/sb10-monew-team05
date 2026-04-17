package com.codeit.monew.domain.comment.entity;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.global.common.base.BaseUpdatableEntity;
import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;
import com.codeit.monew.global.exception.comment.CommentUpdateForbiddenException;
import jakarta.persistence.*;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL") // 데이터 조회할 때마다 쿼리 뒤에 deleted_at IS NULL 조건 자동으로 추가하여 논리 삭제 구현
@SQLDelete(sql = "UPDATE comments SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?") // 삭제 시, deleted_at 필드에 현재 시각을 저장하여 논리 삭제 구현 & version 필드로 낙관적 락 방어하여 동시 삭제 방지
public class Comment extends BaseUpdatableEntity {


  // 기사
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "article_id", nullable = false, updatable = false)
  private Article article;

  // 작성자 id
  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  // 댓글 내용
  @Column(nullable = false, length = 500)
  private String content;

  // 좋아요 수
  @Column(name = "like_count", nullable = false)
  private Long likeCount;

  // 여러 명이 동시에 한 댓글에 좋아요를 누를 때 발생하는 락 경합 방지용 필드, 업데이트 시마다 버전이 증가하여 동시 수정 방지
  @Version // 낙관적 락 방어, JPA가 자동으로 관리하는 버전
  private Long version;

  // 삭제 시각
  @Column(name = "deleted_at")
  private Instant deletedAt;

  // 생성자
  public Comment(Article article, UUID userId, String content) {
    this.article = Objects.requireNonNull(article, "article는 필수입니다.");
    this.userId = Objects.requireNonNull(userId, "userId는 필수입니다.");
    this.content = validateContent(content);
    this.likeCount = 0L;
  }

  // --- 비즈니스 로직 ---
  // 객체가 스스로 권한을 검증할 수 있도록 엔티티 내부에 권한 검증 로직 포함

  // 댓글 수정 시, 작성자와 요청자가 같은지 검증
  public void updateContent(String newContent, UUID requesterId) {
    if (!this.userId.equals(requesterId)) { // 댓글 작성자와 요청자가 다르면 댓글 수정 권한 없음
      throw new CommentUpdateForbiddenException(requesterId); // 댓글 수정 권한 없음 에러 반환
    }
    this.content = validateContent(newContent);
  }

  // 좋아요 등록
  public void increaseLikeCount() {
    this.likeCount++;
  }

  // 좋아요 취소
  public void decreaseLikeCount() {
    if (this.likeCount > 0) {
      this.likeCount--; // 좋아요 수가 0보다 클 때만 감소, 음수 좋아요 방어
    }
  }

  // 댓글 null/blank/길이 검증
  private String validateContent(String value) {
    if (value == null || value.isBlank()) {
      throw new MonewException(ErrorCode.COMMENT_CONTENT_BLANK);
    }
    if (value.length() > 500) {
      throw new MonewException(ErrorCode.COMMENT_CONTENT_TOO_LONG);
    }
    return value;
  }
}