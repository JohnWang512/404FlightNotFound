package com.example.demo.repository;

import com.example.demo.entity.UserTrackedFlight;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserTrackedFlightRepository extends CrudRepository<UserTrackedFlight, Long> {
    List<UserTrackedFlight> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<UserTrackedFlight> findByUserIdAndCallsign(Long userId, String callsign);

    void deleteByUserIdAndCallsign(Long userId, String callsign);

    void deleteByUserId(Long userId);
}
