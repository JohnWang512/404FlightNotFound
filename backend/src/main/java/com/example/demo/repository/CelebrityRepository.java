package com.example.demo.repository;

import com.example.demo.entity.Celebrity;
import org.springframework.data.repository.CrudRepository;
import java.util.List;
import java.util.Optional;

/**
 * Persistence queries for {@link com.example.demo.entity.Celebrity}.
 *
 * Why these methods:
 * - The app can optionally resolve a celebrity by human attributes (name/industry) when seeding or
 *   building richer relationships on the frontend.
 * - Keeping these queries in Spring Data makes them consistent with the rest of the repository layer.
 */
public interface CelebrityRepository extends CrudRepository<Celebrity, Long> {
    /**
     * Find a celebrity by their name.
     *
     * Why it returns {@link Optional}:
     * - There is no enforced uniqueness on `Celebrity.name` in the demo schema; name matches may be absent.
     */
    Optional<Celebrity> findByName(String name);

    /**
     * Find all celebrities matching an industry tag.
     *
     * Why it returns a list:
     * - Multiple celebrities can share the same industry label.
     */
    List<Celebrity> findByIndustry(String industry);
}
