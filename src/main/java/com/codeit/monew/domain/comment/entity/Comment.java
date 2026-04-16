package com.codeit.monew.domain.comment.entity;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;
import jakarta.persistence.*;
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
public class Comment {

  // 댓글 id
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  // 기사 id
  @Column(name = "article_id", nullable = false, updatable = false) // 객체 참조 대신 ID 참조를 사용하여 성능 최적화
  private UUID articleId;

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

  // 생성 시각
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  // 수정 시각
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  // 생성자
  public Comment(UUID articleId, UUID userId, String content) {
    this.articleId = articleId;
    this.userId = userId;
    this.content = content;
    this.likeCount = 0L;

    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  // --- 비즈니스 로직 ---
  // 객체가 스스로 권한을 검증할 수 있도록 엔티티 내부에 권한 검증 로직 포함

  // 댓글 수정 시, 작성자와 요청자가 같은지 검증
  public void updateContent(String newContent, UUID requesterId) {
    if (!this.userId.equals(requesterId)) { // 댓글 작성자와 요청자가 다르면 댓글 수정 권한 없음
      throw new MonewException(ErrorCode.COMMENT_UPDATE_FORBIDDEN); // 댓글 수정 권한 없음 에러 반환
    }
    this.content = newContent;
    this.updatedAt = Instant.now();
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
}