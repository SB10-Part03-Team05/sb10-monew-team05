package com.codeit.monew.domain.comment.repository;

import com.codeit.monew.domain.comment.entity.Comment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

  long countByArticleIdAndDeletedAtIsNull(UUID articleId);
}
