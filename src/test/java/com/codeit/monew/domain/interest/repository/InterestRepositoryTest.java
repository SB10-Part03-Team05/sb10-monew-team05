package com.codeit.monew.domain.interest.repository;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.entity.Keyword;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QueryDslConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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

}
