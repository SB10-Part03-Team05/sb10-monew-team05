package com.codeit.monew.domain.interest.service;

import com.codeit.monew.domain.interest.dto.request.InterestRegisterRequest;
import com.codeit.monew.domain.interest.dto.request.InterestUpdateRequest;
import com.codeit.monew.domain.interest.dto.response.CursorPageResponseInterestDto;
import com.codeit.monew.domain.interest.dto.response.InterestDto;
import com.codeit.monew.domain.interest.dto.response.SubscriptionDto;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.entity.Keyword;
import com.codeit.monew.domain.interest.entity.Subscription;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.interest.repository.KeywordRepository;
import com.codeit.monew.domain.interest.repository.SubscriptionRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.domain.useractivity.event.InterestSubscribedEvent;
import com.codeit.monew.domain.useractivity.event.InterestUnSubscribedEvent;
import com.codeit.monew.global.exception.Interest.AlreadySubscribedException;
import com.codeit.monew.global.exception.Interest.DuplicateInterestException;
import com.codeit.monew.global.exception.Interest.InterestNotFoundException;
import com.codeit.monew.global.exception.Interest.SubscriptionNotFoundException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestService {

  private final InterestRepository interestRepository;
  private final KeywordRepository keywordRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  // 1. 관심사 등록
  @Transactional
  public InterestDto register(InterestRegisterRequest request) {
    log.debug("[INTEREST_CREATE] 관심사 등록 요청: name={}", request.name());

    // 유사도 검사
    List<String> existingNames = interestRepository.findAllNamesWithLock();
    for (String existingName : existingNames) {
      if (calculateSimilarity(request.name(), existingName) >= 0.8) {
        throw new DuplicateInterestException(existingName);
      }
    }

    // 관심사 저장
    Interest interest = Interest.create(request.name());
    interestRepository.save(interest);

    // 키워드 저장
    List<Keyword> keywords = request.keywords().stream()
        .map(name -> Keyword.create(interest, name))
        .toList();
    keywordRepository.saveAll(keywords);
    interest.getKeywords().addAll(keywords);

    log.info("[INTEREST_CREATE] 관심사 등록 완료: interestId={}", interest.getId());
    return InterestDto.from(interest);
  }
  private double calculateSimilarity(String a, String b) {
    a = a.toLowerCase();
    b = b.toLowerCase();
    LevenshteinDistance ld = new LevenshteinDistance();
    int distance = ld.apply(a, b);
    int maxLen = Math.max(a.length(), b.length());
    if (maxLen == 0) return 1.0;
    return 1.0 - ((double) distance / maxLen);
  }

  // 2. 관심사 수정
  @Transactional
  public InterestDto update(UUID interestId, InterestUpdateRequest request) {
    log.debug("[INTEREST_UPDATE] 관심사 수정 요청: interestId={}", interestId);

    // 관심사 존재 여부 확인
    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> new InterestNotFoundException(interestId));

    // 기존 키워드 전체 삭제
    keywordRepository.deleteAllByInterestId(interestId);
    keywordRepository.flush();  // 삭제 먼저 DB에 반영

    // 새 키워드 저장
    List<Keyword> keywords = request.keywords().stream()
        .map(name -> Keyword.create(interest, name))
        .toList();
    keywordRepository.saveAll(keywords);

    // interest 키워드 리스트 갱신
    interest.getKeywords().clear();
    interest.getKeywords().addAll(keywords);

    log.info("[INTEREST_UPDATE] 관심사 수정 완료: interestId={}", interestId);
    return InterestDto.from(interest);
  }

  // 3. 관심사 삭제
  @Transactional
  public void delete(UUID interestId) {
    log.debug("[INTEREST_DELETE] 관심사 삭제 요청: interestId={}", interestId);

    // 관심사 존재 여부 확인
    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> new InterestNotFoundException(interestId));

    // 물리 삭제 (CASCADE로 keyword, subscription 자동 삭제)

    interestRepository.delete(interest);
    log.info("[INTEREST_DELETE] 관심사 삭제 완료: interestId={}", interestId);
  }

  // 4. 관심사 목록 조회
  public CursorPageResponseInterestDto getList(
      String searchKeyword,
      String orderBy,
      String direction,
      String cursor,
      int limit,
      UUID userId
  ) {
    log.debug("[INTEREST_LIST] 관심사 목록 조회 요청: searchKeyword={}, orderBy={}, direction={}, limit={}",
        searchKeyword, orderBy, direction, limit);

    return interestRepository.findInterests(
        searchKeyword, orderBy, direction, cursor, limit, userId
    );
  }

  // 5. 관심사 구독
  @Transactional
  public SubscriptionDto subscribe(UUID interestId, UUID userId) {
    log.debug("[INTEREST_SUBSCRIBE] 구독 요청: interestId={}, userId={}", interestId, userId);

    // 관심사 존재 여부 확인
    Interest interest = interestRepository.findByIdWithKeywords(interestId)
        .orElseThrow(() -> new InterestNotFoundException(interestId));

    // 사용자 존재 여부 확인
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    // 구독 저장 (DB UNIQUE 제약으로 중복 방지)
    Subscription subscription;
    try {
      subscription = Subscription.create(user, interest);
      subscriptionRepository.saveAndFlush(subscription);
    } catch (DataIntegrityViolationException e) {
      if (isDuplicateConstraintViolation(e)) {
        throw new AlreadySubscribedException(userId, interestId);
      }
      throw e;
    }

    // 활동 내역 구독 정보 갱신 로직
    eventPublisher.publishEvent(new InterestSubscribedEvent(
        userId,
        subscription.getId(),
        interest.getId(),
        interest.getName(),
        interest.getKeywords().stream()
            .map(Keyword::getName)
            .toList(), // List<String>으로 변환
        interest.getSubscriberCount() + 1, // DB 락과 별개로 메모리상에서 +1 한 최신값 전달
        subscription.getCreatedAt() != null ? subscription.getCreatedAt() : Instant.now()
    ));

    // 구독자 수 증가
    interestRepository.incrementSubscriberCount(interestId);

    log.info("[INTEREST_SUBSCRIBE] 구독 완료: subscriptionId={}", subscription.getId());
    return SubscriptionDto.from(subscription);
  }

  // UNIQUE 제약 위반 여부 확인 (PostgreSQL SQLState 23505)
  private boolean isDuplicateConstraintViolation(DataIntegrityViolationException e) {
    Throwable cause = e.getMostSpecificCause();
    if (cause instanceof java.sql.SQLException sqlException) {
      return "23505".equals(sqlException.getSQLState());
    }
    return false;
  }

  // 6. 관심사 구독 취소
  @Transactional
  public void unsubscribe(UUID interestId, UUID userId) {
    log.debug("[INTEREST_UNSUBSCRIBE] 구독 취소 요청: interestId={}, userId={}", interestId, userId);

    // 관심사 존재 여부 확인
    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> new InterestNotFoundException(interestId));

    // 구독 여부 확인 및 구독 취소
    int deleted = subscriptionRepository.deleteByUserIdAndInterestId(userId, interestId);
    if (deleted == 0) {
      throw new SubscriptionNotFoundException(userId, interestId);
    }

    eventPublisher.publishEvent(new InterestUnSubscribedEvent(
        userId,
        interestId
    ));

    // 구독자 수 감소
    interestRepository.decrementSubscriberCount(interestId);

    log.info("[INTEREST_UNSUBSCRIBE] 구독 취소 완료: interestId={}, userId={}", interestId, userId);
  }
}
