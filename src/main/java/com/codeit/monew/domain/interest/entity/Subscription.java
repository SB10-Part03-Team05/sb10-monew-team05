package com.codeit.monew.domain.interest.entity;

import com.codeit.monew.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscriptions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "interest_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interest_id", nullable = false)
  private Interest interest;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  // 엔티티가 DB에 저장되는 시점에 자동으로 생성 시각 세팅
  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }

  public static Subscription create(User user, Interest interest) {
    Subscription subscription = new Subscription();
    subscription.user = user;
    subscription.interest = interest;
    return subscription;
  }
}
