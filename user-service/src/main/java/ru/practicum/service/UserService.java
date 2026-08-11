package ru.practicum.service;

import ru.practicum.dto.NewUserRequest;
import ru.practicum.dto.UserDto;
import ru.practicum.dto.param_objects.AdminUserFilter;

import java.util.List;

public interface UserService {
    UserDto createUser(NewUserRequest newUserRequest);

    List<UserDto> getUsers(AdminUserFilter filter);

    UserDto getUserById(Long userId);

    boolean existsById(Long userId);

    List<UserDto> getUsersByIds(List<Long> userIds);

    void deleteUser(Long userId);
}
