package com.codeit.monew.domain.user.service;

import com.codeit.monew.domain.user.dto.UserDto;
import com.codeit.monew.domain.user.dto.UserLoginRequest;
import com.codeit.monew.domain.user.dto.UserRegisterRequest;
import com.codeit.monew.domain.user.dto.UserUpdateRequest;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.mapper.UserMapper;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.domain.useractivity.event.UserRegisteredEvent;
import com.codeit.monew.global.exception.user.DuplicateEmailException;
import com.codeit.monew.global.exception.user.PasswordMismatchException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final ApplicationEventPublisher eventPublisher;

  public UserDto register(UserRegisterRequest request) {
    log.debug("[USER_CREATE] 유저 회원가입 요청: email={}, nickname={}", request.email(), request.nickname());
    existsByEmail(request.email());

    User user = new User(
        request.email(),
        request.nickname(),
        request.password()
    );
    userRepository.save(user);

    eventPublisher.publishEvent(new UserRegisteredEvent(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getCreatedAt() != null ? user.getCreatedAt() : Instant.now()
    ));

    log.info("[USER_CREATE] 유저 생성 완료: userId={}", user.getId());

    return userMapper.toDto(user);
  }

  public UserDto update(UUID userId, UserUpdateRequest request) {
    log.debug("[USER_UPDATE] 유저 수정 요청: userId={}", userId);

    // Soft Delete를 고려해 findByIdAndDeletedAtIsNull()을 호출
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    user.updateNickname(request.nickname());

    log.info("[USER_UPDATE] 유저 수정 완료: userId={}", user.getId());
    return userMapper.toDto(user);
  }

  @CacheEvict(value = "articleList", allEntries = true)
  public void softDelete(UUID userId) {
    log.debug("[USER_DELETE] 유저 논리 삭제 요청: userId={}", userId);

    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
    user.softDelete();
    log.info("[USER_DELETE] 유저 논리 삭제 완료: userId={}", user.getId());
  }

  @CacheEvict(value = "articleList", allEntries = true)
  public void hardDelete(UUID userId) {
    log.debug("[USER_DELETE] 유저 물리 삭제 요청: userId={}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
    userRepository.delete(user);
    log.info("[USER_DELETE] 유저 물리 삭제 완료: userId={}", user.getId());
  }

  public UserDto login(UserLoginRequest request) {
    log.debug("[USER_LOGIN] 유저 로그인 요청");

    User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
        .orElseThrow(() -> new UserNotFoundException(request.email()));
    if (!user.getPassword().equals(request.password())) {
      throw new PasswordMismatchException(request.email());
    }
    log.info("[USER_LOGIN] 유저 로그인 완료: userId={}", user.getId());
    return userMapper.toDto(user);
  }

  private void existsByEmail(String email) {
    boolean exist = userRepository.existsByEmail(email);
    if (exist) {
      throw new DuplicateEmailException(email);
    }
  }
}
