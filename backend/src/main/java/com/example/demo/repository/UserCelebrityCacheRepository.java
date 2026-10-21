package com.example.demo.repository;

import com.example.demo.entity.UserCelebrityCache;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence queries for {@link UserCelebrityCache}.
 *
 * Why:
 * - Supports the "persistent user state" feature: record which celebrities each user has fetched
 *   so subsequent logins can serve flight data from the DB cache without calling AeroAPI.
 */
public interface UserCelebrityCacheRepository extends CrudRepository<UserCelebrityCache, Long> {

    /** All celebrity cache entries for a given user (used to build the login pre-load). */
    List<UserCelebrityCache> findByUserId(Long userId);

    /** Lookup a specific (user, celebrity) pair to determine whether a cache row already exists. */
    Optional<UserCelebrityCache> findByUserIdAndCelebrityId(Long userId, Long celebrityId);
}
