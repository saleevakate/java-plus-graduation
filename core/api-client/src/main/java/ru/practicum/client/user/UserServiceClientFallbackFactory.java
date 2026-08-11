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
                log.error("User service unavailable for getUserById: {}", userId, cause);
                throw new RuntimeException("User service temporarily unavailable");
            }

            @Override
            public Boolean userExists(Long userId) {
                log.error("User service unavailable for userExists: {}", userId, cause);
                return false;
            }

            @Override
            public List<UserDto> getUsersByIds(List<Long> userIds) {
                log.error("User service unavailable for getUsersByIds: {}", userIds, cause);
                throw new RuntimeException("User service temporarily unavailable");
            }

            @Override
            public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
                log.error("User service unavailable for getUsers", cause);
                return List.of();
            }

            @Override
            public UserDto createUser(NewUserRequest request) {
                log.error("User service unavailable for createUser", cause);
                throw new RuntimeException("User service temporarily unavailable");
            }

            @Override
            public void deleteUser(Long userId) {
                log.error("User service unavailable for deleteUser: {}", userId, cause);
            }
        };
    }
}
