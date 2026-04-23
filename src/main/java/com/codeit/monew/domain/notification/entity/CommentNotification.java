package com.codeit.monew.domain.notification.entity;

import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.user.entity.User;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 댓글 좋아요 알림 자식 클래스
@Entity
@DiscriminatorValue("COMMENT") // 자원 타입(resource_type) 칼럼에 "COMMENT" 할당
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentNotification extends Notification{

  // 댓글
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comment_id")
  private Comment comment;

  @Builder(access = AccessLevel.PRIVATE)
  private CommentNotification(User user, String content, Comment comment) {
    super(user, content); // 부모 생성자 호출하여 user, content 할당 및 검증

    if (comment == null) {
      throw new IllegalArgumentException("댓글 알림에는 대상 댓글 엔티티가 필수입니다.");
    }
    this.comment = comment;
  }

  // 정적 팩토리 메서드
  public static CommentNotification create(User user, String content, Comment comment) {
    return CommentNotification.builder()
        .user(user)
        .content(content)
        .comment(comment)
        .build();
  }
}
