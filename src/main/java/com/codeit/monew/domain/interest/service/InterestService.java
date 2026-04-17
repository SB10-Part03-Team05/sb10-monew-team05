package com.codeit.monew.domain.interest.service;

import com.codeit.monew.domain.interest.dto.request.InterestRegisterRequest;
import com.codeit.monew.domain.interest.dto.response.InterestDto;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.entity.Keyword;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.interest.repository.KeywordRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestService {

  private final InterestRepository interestRepository;
  private final KeywordRepository keywordRepository;

  @Transactional
  public InterestDto register(InterestRegisterRequest request) {

    // 유사도 검사
    List<String> existingNames = interestRepository.findAllNamesWithLock();
    for (String existingName : existingNames) {
      // 추후 공통 예외 클래스 생기면 커스텀 예외로 교체 예정 !!
      if (calculateSimilarity(request.name(), existingName) >= 0.8) {
        throw new IllegalArgumentException("유사한 관심사가 이미 존재합니다: " + existingName);
      }
    }

    // 관심사 저장
    Interest interest = Interest.create(request.name());
    interestRepository.save(interest);

    // 키워드 저장
    List<Keyword> keywords = request.keywords().stream()
        .map(name -> Keyword.create(interest, name))
        .toList();

    interest.getKeywords().addAll(keywords);

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
}
