package com.codeit.monew.domain.user.repository;

import com.codeit.monew.domain.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByIdAndDeletedAtIsNull(UUID id);
}
