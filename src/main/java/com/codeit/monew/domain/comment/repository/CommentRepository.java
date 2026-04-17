package com.codeit.monew.domain.comment.repository;

import com.codeit.monew.domain.comment.entity.Comment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

  long countByArticleIdAndDeletedAtIsNull(UUID articleId);

  @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.id = :id AND c.deletedAt IS NULL")
  Optional<Comment> findByIdWithUser(@Param("id") UUID id);
}
