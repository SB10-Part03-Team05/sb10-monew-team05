package com.codeit.monew.domain.user.repository;

import com.codeit.monew.domain.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByIdAndDeletedAtIsNull(UUID id);

  Optional<User> findByEmailAndDeletedAtIsNull(String email);

  boolean existsByEmail(String email);

  List<User> findByDeletedAtBefore(Instant threshold);
}
