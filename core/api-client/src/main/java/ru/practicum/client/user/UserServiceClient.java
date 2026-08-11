package ru.practicum.client.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.config.FeignConfig;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;

import java.util.List;

@FeignClient(
        name = "user-service",
        configuration = FeignConfig.class,
        fallbackFactory = UserServiceClientFallbackFactory.class
)
public interface UserServiceClient {

    @GetMapping("/admin/users/{userId}")
    UserDto getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/admin/users/exists/{userId}")
    Boolean userExists(@PathVariable("userId") Long userId);

    @PostMapping("/admin/users/batch")
    List<UserDto> getUsersByIds(@RequestBody List<Long> userIds);

    @GetMapping("/admin/users")
    List<UserDto> getUsers(@RequestParam(value = "ids", required = false) List<Long> ids,
                           @RequestParam(value = "from", defaultValue = "0") Integer from,
                           @RequestParam(value = "size", defaultValue = "10") Integer size);

    @PostMapping("/admin/users")
    UserDto createUser(@RequestBody NewUserRequest request);

    @DeleteMapping("/admin/users/{userId}")
    void deleteUser(@PathVariable("userId") Long userId);
}
