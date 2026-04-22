package com.codeit.monew.domain.useractivity.service;

import com.codeit.monew.domain.useractivity.dto.UserActivityDto;
import com.codeit.monew.domain.useractivity.entity.UserActivity;
import com.codeit.monew.domain.useractivity.mapper.UserActivityMapper;
import com.codeit.monew.domain.useractivity.repository.UserActivityRepository;
import com.codeit.monew.global.exception.user.UserActivityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityService {

  private final UserActivityRepository userActivityRepository;

  private final UserActivityMapper userActivityMapper;

  @Transactional(readOnly = true)
  public UserActivityDto getUserActivity(UUID userId) {
    UserActivity userActivity = userActivityRepository.findById(userId.toString())
        .orElseThrow(() -> new UserActivityNotFoundException(userId));

    return userActivityMapper.toDto(userActivity);
  }

}
