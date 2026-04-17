package com.codeit.monew.domain.comment.entity;

import com.codeit.monew.global.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
    name = "comment_likes",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_comment_likes_comment_user",
            columnNames = {"comment_id", "user_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike extends BaseEntity {

  // 좋아요 한 댓글
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "comment_id", nullable = false, updatable = false)
  private Comment comment;

  // 좋아요 누른 당사자 id
  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  // 생성자
  public CommentLike(Comment comment, UUID userId) {
    this.comment = comment;
    this.userId = userId;
  }
}