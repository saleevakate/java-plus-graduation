package ru.practicum.service.compilation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.model.Compilation;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.service.event.EventService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventService eventService;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.info("Получение подборок pinned: {}, from: {}, size: {}", pinned, from, size);
        List<Long> compilationIds = compilationRepository.getCompilationIds(pinned, from, size);
        if (compilationIds.isEmpty()) {
            return List.of();
        }

        List<Compilation> compilations = compilationRepository.getCompilations(compilationIds);

        Set<Long> eventIds = compilations.stream()
                .flatMap(c -> c.getEventIds().stream())
                .collect(Collectors.toSet());

        Map<Long, EventShortDto> eventShortDtoMap = getEventShortDtos(new ArrayList<>(eventIds));

        return compilations.stream()
                .map(compilation -> {
                    List<EventShortDto> eventDtos = compilation.getEventIds().stream()
                            .map(eventShortDtoMap::get)
                            .filter(Objects::nonNull)
                            .toList();
                    return compilationMapper.toDto(compilation, eventDtos);
                })
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilation(long compId) {
        log.info("Получение подборки по id: {}", compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с указанным ID не найдена"));

        List<EventShortDto> eventDtos = getEventShortDtos(compilation.getEventIds()).values().stream().toList();

        return compilationMapper.toDto(compilation, eventDtos);
    }

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        log.info("Создание подборки: {}", newCompilationDto);
        Compilation compilation = compilationMapper.toEntity(newCompilationDto);
        if (newCompilationDto.events() != null && !newCompilationDto.events().isEmpty()) {
            validateEventsExist(newCompilationDto.events());
            compilation.setEventIds(newCompilationDto.events());
        } else {
            compilation.setEventIds(List.of());
        }

        Compilation saved = compilationRepository.save(compilation);
        log.info("Подборка создана с id: {}", saved.getId());

        List<EventShortDto> eventDtos = getEventShortDtos(saved.getEventIds()).values().stream().toList();
        return compilationMapper.toDto(saved, eventDtos);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(long compId, UpdateCompilationRequest updateRequest) {
        log.info("Обновление подборки по id: {}, {}", compId, updateRequest);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с указанным ID не найдена"));

        if (updateRequest.title() != null) {
            compilation.setTitle(updateRequest.title());
        }

        if (updateRequest.pinned() != null) {
            compilation.setPinned(updateRequest.pinned());
        }

        if (updateRequest.events() != null) {
            validateEventsExist(updateRequest.events());
            compilation.setEventIds(updateRequest.events());
        }

        Compilation updated = compilationRepository.save(compilation);
        log.info("Подборка обновлена id: {}", updated.getId());

        List<EventShortDto> eventDtos = getEventShortDtos(updated.getEventIds()).values().stream().toList();
        return compilationMapper.toDto(updated, eventDtos);
    }

    @Override
    @Transactional
    public void deleteCompilation(long compId) {
        log.info("Удаление подборки по id: {}", compId);
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с указанным ID не найдена");
        }
        compilationRepository.deleteById(compId);
        log.info("Подборка удалена");
    }

    private Map<Long, EventShortDto> getEventShortDtos(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, EventShortDto> result = new HashMap<>();

        try {
            List<EventShortDto> events = eventService.getEventsByIds(eventIds);
            result = events.stream()
                    .collect(Collectors.toMap(
                            EventShortDto::id,
                            event -> event,
                            (existing, replacement) -> existing
                    ));
            log.debug("Получено {} событий из {}", result.size(), eventIds.size());
        } catch (Exception e) {
            log.error("Ошибка при получении событий из event-service: {}", e.getMessage());
            result = eventIds.stream()
                    .collect(Collectors.toMap(
                            id -> id,
                            id -> new EventShortDto(
                                    "Событие временно недоступно",
                                    null,
                                    0L,
                                    null,
                                    id,
                                    null,
                                    false,
                                    "Событие " + id,
                                    0.0,
                                    0.0
                            )
                    ));
        }

        return result;
    }

    private void validateEventsExist(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return;
        }

        List<Long> existingIds = eventRepository.findAllById(eventIds).stream()
                .map(event -> event.getId())
                .collect(Collectors.toList());

        if (existingIds.size() != eventIds.size()) {
            throw new NotFoundException("Некоторые события не найдены");
        }
    }
}
