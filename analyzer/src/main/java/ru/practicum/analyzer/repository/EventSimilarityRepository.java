package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.EventSimilarity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    Optional<EventSimilarity> findByEventAAndEventB(Long eventA, Long eventB);

    @Query("SELECT e FROM EventSimilarity e WHERE e.eventA = :eventId OR e.eventB = :eventId")
    List<EventSimilarity> findByEventAOrEventB(@Param("eventId") Long eventId);

    @Modifying
    @Query("UPDATE EventSimilarity e SET e.score = :score, e.updatedAt = :time WHERE e.eventA = :eventA AND e.eventB = :eventB")
    void updateScoreAndTime(@Param("eventA") Long eventA,
                            @Param("eventB") Long eventB,
                            @Param("score") Double score,
                            @Param("time") Instant time);
}
