package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.UserActionHistory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserActionHistoryRepository extends JpaRepository<UserActionHistory, Long> {

    Optional<UserActionHistory> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserActionHistory> findAllByUserId(Long userId);

    @Modifying
    @Query("UPDATE UserActionHistory u SET u.weight = :weight, u.lastActionTime = :time WHERE u.userId = :userId AND u.eventId = :eventId")
    void updateWeightAndTime(@Param("userId") Long userId,
                             @Param("eventId") Long eventId,
                             @Param("weight") Double weight,
                             @Param("time") Instant time);

    @Query("SELECT u.eventId FROM UserActionHistory u WHERE u.userId = :userId")
    List<Long> findEventIdsByUserId(@Param("userId") Long userId);
}
