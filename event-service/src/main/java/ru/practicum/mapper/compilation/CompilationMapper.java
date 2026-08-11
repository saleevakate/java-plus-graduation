package ru.practicum.mapper.compilation;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.model.Compilation;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompilationMapper {

    @Mapping(target = "events", ignore = true)
    CompilationDto toDto(Compilation compilation, List<EventShortDto> eventShortDtos);

    Compilation toEntity(NewCompilationDto newCompilationDto);
}
