package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.param_objects.AdminUserFilter;
import ru.practicum.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody @Valid NewUserRequest newUserRequest) {
        log.info("POST /admin/users: {}", newUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(newUserRequest));
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getUsers(
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("GET /admin/users: ids={}, from={}, size={}", ids, from, size);
        AdminUserFilter filter = new AdminUserFilter(from, size, ids);
        return ResponseEntity.ok(userService.getUsers(filter));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long userId) {
        log.info("GET /admin/users/{}", userId);
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @GetMapping("/exists/{userId}")
    public ResponseEntity<Boolean> userExists(@PathVariable Long userId) {
        log.info("GET /admin/users/exists/{}", userId);
        return ResponseEntity.ok(userService.existsById(userId));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<UserDto>> getUsersByIds(@RequestBody List<Long> userIds) {
        log.info("POST /admin/users/batch: {}", userIds);
        return ResponseEntity.ok(userService.getUsersByIds(userIds));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        log.info("DELETE /admin/users/{}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
