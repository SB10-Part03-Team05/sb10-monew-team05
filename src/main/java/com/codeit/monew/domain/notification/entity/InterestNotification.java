package com.codeit.monew.domain.notification.entity;

import com.codeit.monew.domain.interest.entity.Interest;
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

// 관심사 관련 기사 알림 자식 클래스
@Entity
@DiscriminatorValue("INTEREST") // 자원 타입(resource_type) 칼럼에 "INTEREST" 할당
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestNotification extends Notification{

  // 관심사
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interest_id")
  private Interest interest;

  @Builder(access = AccessLevel.PRIVATE)
  private InterestNotification(User user, String content, Interest interest) {
    super(user, content); // 부모 생성자 호출하여 user, content 할당 및 검증

    if (interest == null) {
      throw new IllegalArgumentException("관심사 알림에는 대상 관심사 엔티티가 필수입니다.");
    }
    this.interest = interest;
  }

  // 정적 팩토리 메서드
  public static InterestNotification create(User user, String content, Interest interest) {
    return InterestNotification.builder()
        .user(user)
        .content(content)
        .interest(interest)
        .build();
  }
}
