package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.location.Location;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.model.Event;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "categoryId", source = "category")
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lat", source = "location.lat")
    @Mapping(target = "lon", source = "location.lon")
    Event toEntity(NewEventDto dto);

    @Mapping(target = "categoryId", source = "category")
    @Mapping(target = "lat", source = "location.lat")
    @Mapping(target = "lon", source = "location.lon")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UpdateEventUserRequest dto, @MappingTarget Event event);

    @Mapping(target = "categoryId", source = "category")
    @Mapping(target = "lat", source = "location.lat")
    @Mapping(target = "lon", source = "location.lon")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromAdminDto(UpdateEventAdminRequest dto, @MappingTarget Event event);

    @Mapping(target = "location", expression = "java(toLocation(event))")
    @Mapping(target = "initiator", expression = "java(toUserShortDto(event, initiatorName))")
    @Mapping(target = "category", expression = "java(toCategoryDto(event, categoryName))")
    EventFullDto toFullDto(Event event, Long views, String initiatorName, String categoryName);

    @Mapping(target = "initiator", expression = "java(toUserShortDto(event, initiatorName))")
    @Mapping(target = "category", expression = "java(toCategoryDto(event, categoryName))")
    EventShortDto toShortDto(Event event, Long views, String initiatorName, String categoryName);

    default Location toLocation(Event event) {
        if (event.getLat() == null || event.getLon() == null) {
            return null;
        }
        return new Location(event.getLat().floatValue(), event.getLon().floatValue());
    }

    default UserShortDto toUserShortDto(Event event, String initiatorName) {
        if (event.getInitiatorId() == null) {
            return null;
        }
        return new UserShortDto(event.getInitiatorId(), initiatorName != null ? initiatorName : "Unknown");
    }

    default CategoryDto toCategoryDto(Event event, String categoryName) {
        if (event.getCategoryId() == null) {
            return null;
        }
        return new CategoryDto(event.getCategoryId(), categoryName != null ? categoryName : "Unknown");
    }
}
