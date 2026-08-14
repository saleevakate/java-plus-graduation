package ru.practicum.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.param_objects.AdminUserFilter;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.QUser;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest newUserRequest) {
        log.info("Создание пользователя: {}", newUserRequest);

        if (userRepository.existsByEmail(newUserRequest.email())) {
            throw new ConflictException("Пользователь с email " + newUserRequest.email() + " уже существует");
        }

        User user = userMapper.toUser(newUserRequest);
        User saved = userRepository.save(user);
        log.info("Пользователь создан с id: {}", saved.getId());

        return userMapper.toDto(saved);
    }

    @Override
    public List<UserDto> getUsers(AdminUserFilter filter) {
        log.info("Получение пользователей с фильтром: {}", filter);

        int page = filter.from() / filter.size();
        Pageable pageable = PageRequest.of(page, filter.size());

        List<User> users;
        if (filter.ids().isEmpty()) {
            users = userRepository.findAll(pageable).getContent();
        } else {
            Predicate predicate = buildPredicate(filter);
            users = userRepository.findAll(predicate, pageable).getContent();
        }

        log.info("Найдено {} пользователей", users.size());
        return users.stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    public UserDto getUserById(Long userId) {
        log.info("Получение пользователя по id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        return userMapper.toDto(user);
    }

    @Override
    public boolean existsById(Long userId) {
        log.info("Проверка существования пользователя по id: {}", userId);
        return userRepository.existsById(userId);
    }

    @Override
    public List<UserDto> getUsersByIds(List<Long> userIds) {
        log.info("Получение пользователей по списку ids: {}", userIds);
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<User> users = userRepository.findAllById(userIds);
        log.info("Найдено {} пользователей из {}", users.size(), userIds.size());
        return users.stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Удаление пользователя с id: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        userRepository.deleteById(userId);
        log.info("Пользователь с id {} удален", userId);
    }

    private Predicate buildPredicate(AdminUserFilter filter) {
        QUser qUser = QUser.user;
        BooleanBuilder builder = new BooleanBuilder();

        if (filter.ids() != null && !filter.ids().isEmpty()) {
            builder.and(qUser.id.in(filter.ids()));
        }

        return builder.getValue();
    }
}
