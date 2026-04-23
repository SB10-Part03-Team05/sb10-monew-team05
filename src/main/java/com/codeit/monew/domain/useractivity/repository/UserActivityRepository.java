package com.codeit.monew.domain.useractivity.repository;

import com.codeit.monew.domain.useractivity.entity.UserActivity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserActivityRepository extends MongoRepository<UserActivity, String> {

}
