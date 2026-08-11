package ru.practicum.service.event;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.client.request.RequestServiceClient;
import ru.practicum.client.user.UserServiceClient;
import ru.practicum.dto.event.*;
import ru.practicum.dto.event.param_objects.AdminEventsFilter;
import ru.practicum.dto.event.param_objects.PublicEventsFilter;
import ru.practicum.dto.stats.EndpointHit;
import ru.practicum.dto.stats.ViewStats;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.UserServiceUnavailableException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.model.QEvent;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    @PersistenceContext
    private EntityManager entityManager;

    private JPAQueryFactory queryFactory;

    @PostConstruct
    public void init() {
        queryFactory = new JPAQueryFactory(entityManager);
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LocalDateTime STATS_RANGE_START = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
    private static final String APP_NAME = "ewm-event-service";

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final UserServiceClient userServiceClient;
    private final RequestServiceClient requestServiceClient;
    private final StatsClient statsClient;

    private Event getEventByIdOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не существует"));
    }

    private Event getOwnedEventOrThrow(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    private void validateUser(Long userId) {
        try {
            Boolean exists = userServiceClient.userExists(userId);
            if (!exists) {
                throw new NotFoundException("Пользователь с id=" + userId + " не найден");
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке пользователя в user-service: {}", e.getMessage());
            throw new UserServiceUnavailableException("Сервис пользователей временно недоступен");
        }
    }

    private String getUserName(Long userId) {
        try {
            return userServiceClient.getUserById(userId).name();
        } catch (Exception e) {
            log.error("Ошибка при получении пользователя из user-service: {}", e.getMessage());
            return "Unknown User " + userId;
        }
    }

    private Map<Long, String> getUserNames(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return userServiceClient.getUsersByIds(userIds)
                    .stream()
                    .collect(Collectors.toMap(
                            ru.practicum.dto.user.UserDto::id,
                            ru.practicum.dto.user.UserDto::name,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("Ошибка при получении пользователей из user-service: {}", e.getMessage());
            return userIds.stream()
                    .collect(Collectors.toMap(id -> id, id -> "Unknown User " + id));
        }
    }

    private String getCategoryName(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .map(Category::getName)
                .orElse("Unknown");
    }

    private Map<Long, String> getCategoryNames(List<Long> categoryIds) {
        return categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private void saveHit(HttpServletRequest request) {
        saveHit(request, request.getRequestURI());
    }

    private void saveHit(HttpServletRequest request, String uri) {
        try {
            String ip = request.getRemoteAddr();
            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            EndpointHit hit = new EndpointHit(null, APP_NAME, uri, ip, timestamp);
            statsClient.saveHit(hit);
            log.debug("Hit saved: {}", hit);
        } catch (Exception e) {
            log.error("Failed to save hit: {}", e.getMessage());
        }
    }

    private boolean sortByViews(PublicEventsFilter filter) {
        return filter.sort() != null && filter.sort().equals(EventSort.VIEWS);
    }

    private Predicate predicateFromFilter(PublicEventsFilter filter) {
        QEvent event = QEvent.event;
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(event.state.eq(String.valueOf(EventState.PUBLISHED)));

        if (filter.text() != null && !filter.text().isBlank()) {
            String searchText = "%" + filter.getNormalizedText() + "%";
            BooleanExpression textCondition = Expressions.stringTemplate(
                            "LOWER({0})", event.annotation
                    ).like(searchText)
                    .or(Expressions.stringTemplate(
                            "LOWER({0})", event.description
                    ).like(searchText));
            builder.and(textCondition);
        }

        if (filter.categories() != null && !filter.categories().isEmpty()) {
            builder.and(event.categoryId.in(filter.categories()));
        }

        if (filter.paid() != null) {
            builder.and(event.paid.eq(filter.paid()));
        }

        LocalDateTime startDate = filter.getRangeStartDateTime();
        LocalDateTime endDate = filter.getRangeEndDateTime();

        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new ValidationException("Начало события не может быть позже завершения события");
            }
            builder.and(event.eventDate.between(startDate, endDate));
        } else if (startDate != null) {
            builder.and(event.eventDate.after(startDate));
        } else if (endDate != null) {
            builder.and(event.eventDate.before(endDate));
        }

        if (filter.onlyAvailable()) {
            builder.and(event.confirmedRequests.lt(event.participantLimit));
        }

        return builder.getValue();
    }

    private Map<Long, Long> getViewsFromStats(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            String start = STATS_RANGE_START.format(DATE_FORMATTER);
            String end = LocalDateTime.now().format(DATE_FORMATTER);

            List<ViewStats> stats = statsClient.getHits(start, end, uris, true);

            return stats.stream()
                    .collect(Collectors.toMap(
                            stat -> Long.parseLong(stat.uri().replace("/events/", "")),
                            ViewStats::hits
                    ));
        } catch (Exception e) {
            log.error("Failed to get views from stats service: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @Override
    public EventFullDto getEventById(Long eventId, HttpServletRequest request) {
        log.info("Получение события по id: {}", eventId);

        saveHit(request);

        Event event = getEventByIdOrThrow(eventId);
        if (!event.getState().equals(EventState.PUBLISHED.name())) {
            throw new NotFoundException("Событие не опубликовано");
        }

        Long views = getViewsFromStats(List.of(eventId)).getOrDefault(eventId, 0L);
        String initiatorName = getUserName(event.getInitiatorId());
        String categoryName = getCategoryName(event.getCategoryId());

        return eventMapper.toFullDto(event, views, initiatorName, categoryName);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        log.info("Получение событий пользователя: userId={}, from={}, size={}", userId, from, size);
        validateUser(userId);

        List<Event> events = eventRepository.findAllByInitiatorId(userId, PageRequest.of(from / size, size))
                .getContent();

        Map<Long, Long> viewsMap = getViewsFromStats(events.stream().map(Event::getId).toList());
        Map<Long, String> userNames = getUserNames(events.stream().map(Event::getInitiatorId).toList());
        Map<Long, String> categoryNames = getCategoryNames(events.stream().map(Event::getCategoryId).toList());

        return events.stream()
                .map(event -> eventMapper.toShortDto(
                        event,
                        viewsMap.getOrDefault(event.getId(), 0L),
                        userNames.get(event.getInitiatorId()),
                        categoryNames.get(event.getCategoryId())
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("Создание события: {}", newEventDto);
        validateUser(userId);

        if (newEventDto.eventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата события не может быть раньше, чем через два часа от текущего момента");
        }

        if (!categoryRepository.existsById(newEventDto.category())) {
            throw new NotFoundException("Категория с id=" + newEventDto.category() + " не найдена");
        }

        Event event = eventMapper.toEntity(newEventDto);
        event.setInitiatorId(userId);
        event.setState(EventState.PENDING.name());
        event.setCreatedOn(LocalDateTime.now());
        event.setConfirmedRequests(0L);

        if (event.getPaid() == null) {
            event.setPaid(false);
        }
        if (event.getParticipantLimit() == null) {
            event.setParticipantLimit(0);
        }
        if (event.getRequestModeration() == null) {
            event.setRequestModeration(true);
        }

        Event saved = eventRepository.save(event);
        log.info("Событие создано с id: {}", saved.getId());

        String initiatorName = getUserName(userId);
        String categoryName = getCategoryName(saved.getCategoryId());

        return eventMapper.toFullDto(saved, 0L, initiatorName, categoryName);
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        log.info("Получение события пользователя: userId={}, eventId={}", userId, eventId);
        validateUser(userId);
        Event event = getOwnedEventOrThrow(userId, eventId);

        Long views = getViewsFromStats(List.of(eventId)).getOrDefault(eventId, 0L);
        String initiatorName = getUserName(event.getInitiatorId());
        String categoryName = getCategoryName(event.getCategoryId());

        return eventMapper.toFullDto(event, views, initiatorName, categoryName);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("Обновление события пользователем: userId={}, eventId={}", userId, eventId);
        validateUser(userId);
        Event event = getOwnedEventOrThrow(userId, eventId);

        if (!EventState.PENDING.name().equals(event.getState())
                && !EventState.CANCELED.name().equals(event.getState())) {
            throw new ConflictException("Изменить можно только ожидающие или отмененные события");
        }

        if (updateRequest.eventDate() != null
                && updateRequest.eventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата события не может быть раньше, чем через два часа от текущего момента");
        }

        eventMapper.updateEntityFromDto(updateRequest, event);

        if (updateRequest.category() != null) {
            if (!categoryRepository.existsById(updateRequest.category())) {
                throw new NotFoundException("Категория с id=" + updateRequest.category() + " не найдена");
            }
            event.setCategoryId(updateRequest.category());
        }

        if (updateRequest.stateAction() != null) {
            event.setState(updateRequest.stateAction() == UserStateAction.SEND_TO_REVIEW
                    ? EventState.PENDING.name() : EventState.CANCELED.name());
        }

        Event saved = eventRepository.save(event);
        Long views = getViewsFromStats(List.of(saved.getId())).getOrDefault(saved.getId(), 0L);
        String initiatorName = getUserName(saved.getInitiatorId());
        String categoryName = getCategoryName(saved.getCategoryId());

        return eventMapper.toFullDto(saved, views, initiatorName, categoryName);
    }

    @Override
    public List<EventShortDto> getPublishedEvents(PublicEventsFilter filter, HttpServletRequest request) {
        log.info("Поиск опубликованных событий с фильтром: {}", filter);

        saveHit(request);

        Predicate predicate = predicateFromFilter(filter);

        List<Event> events = queryFactory
                .selectFrom(QEvent.event)
                .where(predicate)
                .fetch();

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        for (Long eventId : eventIds) {
            saveHit(request, "/events/" + eventId);
        }

        Map<Long, Long> views = getViewsFromStats(eventIds);
        Map<Long, String> userNames = getUserNames(events.stream().map(Event::getInitiatorId).toList());
        Map<Long, String> categoryNames = getCategoryNames(events.stream().map(Event::getCategoryId).toList());

        return events.stream()
                .map(event -> eventMapper.toShortDto(
                        event,
                        views.getOrDefault(event.getId(), 0L),
                        userNames.get(event.getInitiatorId()),
                        categoryNames.get(event.getCategoryId())
                ))
                .sorted(sortByViews(filter)
                        ? Comparator.comparingLong(EventShortDto::views).reversed()
                        : Comparator.comparing(EventShortDto::eventDate))
                .skip(filter.from())
                .limit(filter.size())
                .toList();
    }

    @Override
    public List<EventFullDto> getAdminEvents(AdminEventsFilter filter) {
        log.info("Поиск событий с фильтром: {}", filter);
        QEvent event = QEvent.event;
        BooleanBuilder builder = new BooleanBuilder();

        if (!filter.users().isEmpty()) {
            builder.and(event.initiatorId.in(filter.users()));
        }
        if (!filter.states().isEmpty()) {
            builder.and(event.state.in(filter.states()));
        }
        if (!filter.categories().isEmpty()) {
            builder.and(event.categoryId.in(filter.categories()));
        }

        LocalDateTime startDate = filter.getRangeStartDateTime();
        LocalDateTime endDate = filter.getRangeEndDateTime();
        if (startDate != null) {
            builder.and(event.eventDate.goe(startDate));
        }
        if (endDate != null) {
            builder.and(event.eventDate.loe(endDate));
        }

        List<Event> events = queryFactory
                .selectFrom(event)
                .where(builder.getValue())
                .offset(filter.from())
                .limit(filter.size())
                .fetch();

        Map<Long, Long> views = getViewsFromStats(events.stream().map(Event::getId).toList());
        Map<Long, String> userNames = getUserNames(events.stream().map(Event::getInitiatorId).toList());
        Map<Long, String> categoryNames = getCategoryNames(events.stream().map(Event::getCategoryId).toList());

        return events.stream()
                .map(e -> eventMapper.toFullDto(
                        e,
                        views.getOrDefault(e.getId(), 0L),
                        userNames.get(e.getInitiatorId()),
                        categoryNames.get(e.getCategoryId())
                ))
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request) {
        log.info("Обновление события администратором: eventId={}", eventId);
        Event event = getEventByIdOrThrow(eventId);

        if (request.eventDate() != null && request.eventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Дата начала изменяемого события должна быть не ранее чем за час от даты публикации");
        }

        if (request.stateAction() != null) {
            if (request.stateAction() == AdminStateAction.PUBLISH_EVENT) {
                if (!EventState.PENDING.name().equals(event.getState())) {
                    throw new ConflictException("Нельзя опубликовать событие в статусе: " + event.getState());
                }
                event.setState(EventState.PUBLISHED.name());
                event.setPublishedOn(LocalDateTime.now());
            } else if (request.stateAction() == AdminStateAction.REJECT_EVENT) {
                if (EventState.PUBLISHED.name().equals(event.getState())) {
                    throw new ConflictException("Нельзя отклонить событие, так как оно уже опубликовано");
                }
                event.setState(EventState.CANCELED.name());
            }
        }

        eventMapper.updateEntityFromAdminDto(request, event);

        if (request.category() != null) {
            if (!categoryRepository.existsById(request.category())) {
                throw new NotFoundException("Категория с id=" + request.category() + " не найдена");
            }
            event.setCategoryId(request.category());
        }

        Event saved = eventRepository.save(event);
        Long views = getViewsFromStats(List.of(saved.getId())).getOrDefault(saved.getId(), 0L);
        String initiatorName = getUserName(saved.getInitiatorId());
        String categoryName = getCategoryName(saved.getCategoryId());

        return eventMapper.toFullDto(saved, views, initiatorName, categoryName);
    }

    @Override
    public Map<Long, Long> getViews(List<Long> eventIds) {
        return getViewsFromStats(eventIds);
    }

    @Override
    public boolean eventExists(Long eventId) {
        return eventRepository.existsById(eventId);
    }

    @Override
    public boolean isEventOwner(Long userId, Long eventId) {
        return eventRepository.findById(eventId)
                .map(event -> event.getInitiatorId().equals(userId))
                .orElse(false);
    }

    @Override
    public boolean isEventPublished(Long eventId) {
        return eventRepository.findById(eventId)
                .map(event -> EventState.PUBLISHED.name().equals(event.getState()))
                .orElse(false);
    }

    @Override
    public int getParticipantLimit(Long eventId) {
        return eventRepository.findById(eventId)
                .map(Event::getParticipantLimit)
                .orElse(0);
    }

    @Override
    public List<EventShortDto> getEventsByIds(List<Long> eventIds) {
        log.info("Получение событий по списку ids: {}", eventIds);

        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }
        List<Event> events = eventRepository.findAllById(eventIds);

        if (events.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> viewsMap = getViewsFromStats(eventIds);
        Map<Long, String> userNames = getUserNames(events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toList()));

        Map<Long, String> categoryNames = getCategoryNames(events.stream()
                .map(Event::getCategoryId)
                .collect(Collectors.toList()));

        return events.stream()
                .map(event -> eventMapper.toShortDto(
                        event,
                        viewsMap.getOrDefault(event.getId(), 0L),
                        userNames.get(event.getInitiatorId()),
                        categoryNames.get(event.getCategoryId())
                ))
                .collect(Collectors.toList());
    }
}
