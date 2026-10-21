package com.example.demo.repository;

import com.example.demo.entity.AppUser;
import org.springframework.data.repository.CrudRepository;
import java.util.Optional;

/**
 * Persistence queries for {@link com.example.demo.entity.AppUser}.
 *
 * Why:
 * - Username-based lookup is helpful during seeding/testing, and it mirrors the shape of many real login systems.
 * - This is still demo-level: there is no hashing/validation in the repository layer.
 */
public interface UserRepository extends CrudRepository<AppUser, Long> {
    /**
     * Find a user by username.
     *
     * Why returns {@link Optional}:
     * - In the demo schema, username is not enforced as UNIQUE, so a match is not guaranteed.
     */
    Optional<AppUser> findByUname(String uname);
}
