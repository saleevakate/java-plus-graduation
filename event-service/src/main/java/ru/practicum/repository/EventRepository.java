package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Event;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {

    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);

    @Query("""
            SELECT e.id, e.annotation, e.categoryId, e.confirmedRequests,
                   e.eventDate, e.initiatorId, e.paid, e.title,
                   (6371000 * acos(
                       cos(radians(:lat)) * cos(radians(e.lat)) *
                       cos(radians(e.lon) - radians(:lon)) +
                       sin(radians(:lat)) * sin(radians(e.lat))
                   )) AS distance
            FROM Event e
            WHERE e.state = 'PUBLISHED'
              AND (6371000 * acos(
                cos(radians(:lat)) * cos(radians(e.lat)) *
                cos(radians(e.lon) - radians(:lon)) +
                sin(radians(:lat)) * sin(radians(e.lat))
              )) <= :radiusMeters
            ORDER BY distance
            """)
    List<Object[]> findEventsWithinRadius(
            @Param("radiusMeters") Double radiusMeters,
            @Param("lat") Double lat,
            @Param("lon") Double lon,
            Pageable pageable
    );
}
