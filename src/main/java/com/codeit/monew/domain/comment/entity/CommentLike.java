package com.codeit.monew.domain.comment.entity;

import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

  // 좋아요 누른 유저
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  private User user;

  // 생성자
  public CommentLike(Comment comment, User user) {
    this.comment = comment;
    this.user = user;
  }
}