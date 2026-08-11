package ru.practicum.client.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;

import java.util.List;

@Slf4j
@Component
public class UserServiceClientFallbackFactory implements FallbackFactory<UserServiceClient> {

    @Override
    public UserServiceClient create(Throwable cause) {
        return new UserServiceClient() {
            @Override
            public UserDto getUserById(Long userId) {
                log.error("Сервис пользователей недоступен для getUserById: {}", userId, cause);
                throw new RuntimeException("Сервис пользователей временно недоступен");
            }

            @Override
            public Boolean userExists(Long userId) {
                log.error("Сервис пользователей недоступен для userExists: {}", userId, cause);
                return false;
            }

            @Override
            public List<UserDto> getUsersByIds(List<Long> userIds) {
                log.error("Сервис пользователей недоступен для getUsersByIds: {}", userIds, cause);
                throw new RuntimeException("Сервис пользователей временно недоступен");
            }

            @Override
            public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
                log.error("Сервис пользователей недоступен для getUsers", cause);
                return List.of();
            }

            @Override
            public UserDto createUser(NewUserRequest request) {
                log.error("Сервис пользователей недоступен для createUser", cause);
                throw new RuntimeException("Сервис пользователей временно недоступен");
            }

            @Override
            public void deleteUser(Long userId) {
                log.error("Сервис пользователей недоступен для deleteUser: {}", userId, cause);
            }
        };
    }
}
