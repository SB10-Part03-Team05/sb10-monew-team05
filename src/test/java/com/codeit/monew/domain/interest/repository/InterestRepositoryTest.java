package com.codeit.monew.domain.interest.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.interest.dto.response.CursorPageResponseInterestDto;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.entity.Keyword;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QueryDslConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, QueryDslConfig.class})
class InterestRepositoryTest {

  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private KeywordRepository keywordRepository;

  @Autowired
  private SubscriptionRepository subscriptionRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TestEntityManager testEntityManager;

  // 매 테스트 전 이전 테스트 데이터 비우고 시작
  @BeforeEach
  void setUp() {
    subscriptionRepository.deleteAll();
    keywordRepository.deleteAll();
    interestRepository.deleteAll();
    userRepository.deleteAll();
  }

  private Interest createInterest(String name, List<String> keywords) {
    Interest interest = Interest.create(name);
    interestRepository.save(interest);
    keywords.forEach(k -> keywordRepository.save(Keyword.create(interest, k)));
    testEntityManager.flush();
    testEntityManager.clear();
    return interest;
  }

  private User createUser(String email) {
    User user = new User(email, "nickname", "password");
    return userRepository.save(user);
  }

  // 1. 검색
  @Nested
  @DisplayName("관심사 검색 테스트")
  class search {

    @Test
    @DisplayName("검색어 없으면 전체 조회를 진행한다.")
    void success_find_all_when_no_keyword() {
      // given
      createInterest("스포츠", List.of("축구", "야구"));
      createInterest("경제", List.of("주식", "코인"));
      createInterest("IT", List.of("개발", "AI"));
      UUID userId = createUser("test@email.com").getId();

      testEntityManager.flush();
      testEntityManager.clear();

      // when
      CursorPageResponseInterestDto result = interestRepository.findInterests(
          null, "name", "ASC", null, 10, userId
      );

      // then
      assertThat(result.content()).hasSize(3);
      assertThat(result.totalElements()).isEqualTo(3);
      assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("키워드로 부분일치 검색이 가능하다.")
    void success_search_by_keyword() {
      // given
      createInterest("스포츠", List.of("축구", "야구"));
      createInterest("경제", List.of("주식", "코인"));
      UUID userId = createUser("test@email.com").getId();

      testEntityManager.flush();
      testEntityManager.clear();

      // when
      CursorPageResponseInterestDto result = interestRepository.findInterests(
          "축구", "name", "ASC", null, 10, userId
      );

      // then
      assertThat(result.content()).hasSize(1);
      assertThat(result.content().get(0).name()).isEqualTo("스포츠");
    }

    @Test
    @DisplayName("매칭되는 결과 없으면 빈 리스트가 반환된다.")
    void success_return_empty_when_no_match() {
      // given
      createInterest("스포츠", List.of("축구", "야구"));
      UUID userId = createUser("test@email.com").getId();

      testEntityManager.flush();
      testEntityManager.clear();

      // when
      CursorPageResponseInterestDto result = interestRepository.findInterests(
          "없는검색어", "name", "ASC", null, 10, userId
      );

      // then
      assertThat(result.content()).isEmpty();
      assertThat(result.totalElements()).isEqualTo(0);
      assertThat(result.hasNext()).isFalse();
    }
  }

  // 2. 정렬
  @Nested
  @DisplayName("관심사 정렬 테스트")
  class sort {

    @Test
    @DisplayName("name이 ASC 정렬된다.")
    void success_sort_by_name_asc() {
      // given
      createInterest("축구", List.of("공"));
      createInterest("가나다", List.of("가"));
      createInterest("마바사", List.of("마"));
      UUID userId = createUser("test@email.com").getId();

      testEntityManager.flush();
      testEntityManager.clear();

      // when
      CursorPageResponseInterestDto result = interestRepository.findInterests(
          null, "name", "ASC", null, 10, userId
      );

      // then
      assertThat(result.content()).extracting("name")
          .containsExactly("가나다", "마바사", "축구");
    }

    @Test
    @DisplayName("name이 DESC 정렬된다.")
    void success_sort_by_name_desc() {
      // given
      createInterest("축구", List.of("공"));
      createInterest("가나다", List.of("가"));
      createInterest("마바사", List.of("마"));
      UUID userId = createUser("test@email.com").getId();

      testEntityManager.flush();
      testEntityManager.clear();

      // when
      CursorPageResponseInterestDto result = interestRepository.findInterests(
          null, "name", "DESC", null, 10, userId
      );

      // then
      assertThat(result.content()).extracting("name")
          .containsExactly("축구", "마바사", "가나다");
    }

    @Test
    @DisplayName("subscriberCount이 ASC 정렬된다.")
    void success_sort_by_subscriber_count_asc() {
      // given
      Interest interest1 = createInterest("스포츠", List.of("축구"));
      Interest interest2 = createInterest("경제", List.of("주식"));
      Interest interest3 = createInterest("IT", List.of("개발"));

      // subscriberCount 직접 설정
      interestRepository.incrementSubscriberCount(interest1.getId());
      interestRepository.incrementSubscriberCount(interest1.getId());
      interestRepository.incrementSubscriberCount(interest2.getId());
      UUID userId = createUser("test@email.com").getId();

      testEntityManager.flush();
      testEntityManager.clear();

      // when
      CursorPageResponseInterestDto result = interestRepository.findInterests(
          null, "subscriberCount", "ASC", null, 10, userId
      );

      // then
      assertThat(result.content()).extracting("name")
          .containsExactly("IT", "경제", "스포츠");
    }

    @Test
    @DisplayName("subscriberCount이 DESC 정렬된다.")
    void success_sort_by_subscriber_count_desc() {
      // given
      Interest interest1 = createInterest("스포츠", List.of("축구"));
      Interest interest2 = createInterest("경제", List.of("주식"));
      Interest interest3 = createInterest("IT", List.of("개발"));

      interestRepository.incrementSubscriberCount(interest1.getId());
      interestRepository.incrementSubscriberCount(interest1.getId());
      interestRepository.incrementSubscriberCount(interest2.getId());
      UUID userId = createUser("test@email.com").getId();

      testEntityManager.flush();
      testEntityManager.clear();

      // when
      CursorPageResponseInterestDto result = interestRepository.findInterests(
          null, "subscriberCount", "DESC", null, 10, userId
      );

      // then
      assertThat(result.content()).extracting("name")
          .containsExactly("스포츠", "경제", "IT");
    }
  }
  // 3. 커서 페이지네이션
  // 4. 구독


}
