package com.codeit.monew.domain.notification.entity;

import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.common.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user_confirmed_created", columnList = "user_id, confirmed, created_at DESC")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // resource 필드에 기사/댓글 모두 할당 가능하게 하기 위해 자식 클래스 생성
@DiscriminatorColumn(name = "resource_type") // 자원 타입
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseUpdatableEntity {

  // 사용자
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  // 알림 내용
  @Column(name = "content", nullable = false, length = 500)
  private String content;

  // 알림 확인 여부
  @Column(name = "confirmed", nullable = false)
  private boolean confirmed = false;

  // 자식 클래스에서 호출할 생성자
  protected Notification(User user, String content) {
    validateNotification(user, content); // 생성 시 내부 검증 로직 실행
    this.user = user;
    this.content = content;
    this.confirmed = false;
  }

  // 엔티티 무결성 검증 로직
  private void validateNotification(User user, String content) {
    if (user == null || content == null || content.isBlank()) {
      throw new IllegalArgumentException("알림 생성 시 필수 값이 누락되었습니다.");
    }
    if (content.length() > 500) {
      throw new IllegalArgumentException("알림 내용이 제한 길이를 초과했습니다.");
    }
  }

  // 비즈니스 로직 - 알림 읽음 처리
  public void confirm() {
    this.confirmed = true;
  }
}
