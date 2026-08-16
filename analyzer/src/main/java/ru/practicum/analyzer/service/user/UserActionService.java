package ru.practicum.analyzer.service.user;

import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface UserActionService {

    void processUserAction(UserActionAvro event);
}
