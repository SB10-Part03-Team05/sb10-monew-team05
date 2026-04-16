package com.codeit.monew.domain.user.mapper;

import com.codeit.monew.domain.user.dto.UserDto;
import com.codeit.monew.domain.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
  UserDto toDto(User user);
}
