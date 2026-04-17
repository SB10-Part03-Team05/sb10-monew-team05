package com.codeit.monew.domain.user.service;

import com.codeit.monew.domain.user.dto.UserDto;
import com.codeit.monew.domain.user.dto.UserRegisterRequest;
import com.codeit.monew.domain.user.dto.UserUpdateRequest;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.mapper.UserMapper;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.exception.MonewException;
import com.codeit.monew.global.exception.user.DuplicateEmailException;
import com.codeit.monew.global.exception.user.UserAccessDeniedException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  public UserDto register(UserRegisterRequest request) {
    log.debug("[USER_CREATE] 유저 회원가입 요청: email={}, nickname={}", request.email(), request.nickname());
    existsByEmail(request.email());

    User user = new User(
        request.email(),
        request.nickname(),
        request.password()
    );
    userRepository.save(user);

    log.info("[USER_CREATE] 유저 생성 완료: userId={}", user.getId());

    return userMapper.toDto(user);
  }

  public UserDto update(UUID userId, UUID requestUserId, UserUpdateRequest request) {
    log.debug("[USER_UPDATE] 유저 수정 요청: userId={}", userId);

    // URI에 포함된 userId와 헤더에 포함된 requestUserId를 비교해서 다르다면 예외를 던진다.
    if (!userId.equals(requestUserId)) {
      throw new UserAccessDeniedException(requestUserId);
    }

    // Soft Delete를 고려해 findByIdAndDeletedAtIsNull()을 호출
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    user.updateNickname(request.nickname());

    log.info("[USER_UPDATE] 유저 수정 완료: userId={}", user.getId());
    return userMapper.toDto(user);
  }

  private void existsByEmail(String email) {
    boolean exist = userRepository.existsByEmail(email);
    if (exist) {
      throw new DuplicateEmailException(email);
    }
  }
}
