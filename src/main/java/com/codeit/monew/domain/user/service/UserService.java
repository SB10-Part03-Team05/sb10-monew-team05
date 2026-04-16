package com.codeit.monew.domain.user.service;

import com.codeit.monew.domain.user.dto.UserDto;
import com.codeit.monew.domain.user.dto.UserRegisterRequest;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.mapper.UserMapper;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    existsByEmail(request.email());

    User user = new User(
        request.email(),
        request.nickname(),
        request.password()
    );
    userRepository.save(user);

    log.info("유저 생성 완료: userId={}", user.getId());

    return userMapper.toDto(user);
  }

  private void existsByEmail(String email) {
    boolean exist = userRepository.existsByEmail(email);
    if (exist) {
      //임시로 MonewException 클래스 사용, 추후 UserException 클래스 추가시 변경
      throw new MonewException(ErrorCode.DUPLICATE_EMAIL);
    }
  }
}
