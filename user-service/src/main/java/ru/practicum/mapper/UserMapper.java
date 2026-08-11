package ru.practicum.mapper;

import org.mapstruct.Mapper;


import ru.practicum.dto.NewUserRequest;
import ru.practicum.dto.UserDto;
import ru.practicum.dto.UserShortDto;
import ru.practicum.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(NewUserRequest newUserRequest);

    UserDto toDto(User user);

    UserShortDto toShortDto(User user);
}