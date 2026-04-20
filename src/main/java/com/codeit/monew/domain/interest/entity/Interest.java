package com.codeit.monew.domain.interest.entity;

import com.codeit.monew.global.common.base.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interest extends BaseEntity {

  @Column(nullable = false, unique = true, length = 50)
  private String name;

  @Column(nullable = false)
  private long subscriberCount = 0;

  @OneToMany(mappedBy = "interest", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Keyword> keywords = new ArrayList<>();

  // 관심사 생성
  public static Interest create(String name) {
    Interest interest = new Interest();
    interest.name = name;
    return interest;
  }

  public void increaseSubscriberCount() {
    this.subscriberCount++;
  }

  // 구독자 수가 0 미만으로 내려가지 않도록 방어
  public void decreaseSubscriberCount() {
    if (this.subscriberCount > 0) this.subscriberCount--;
  }
}
