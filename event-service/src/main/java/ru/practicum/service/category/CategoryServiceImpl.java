package ru.practicum.service.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.model.Category;
import ru.practicum.repository.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        log.info("Получение категорий from: {}, size: {}", from, size);
        return categoryRepository.findAll(PageRequest.of(from / size, size))
                .stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        log.info("Получение категории по id: {}", catId);
        Category category = getCatById(catId);
        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.info("Создание категории: {}", newCategoryDto.name());
        if (categoryRepository.existsByName(newCategoryDto.name())) {
            throw new ConflictException("Категория с именем '" + newCategoryDto.name() + "' уже существует");
        }
        Category category = categoryMapper.toEntity(newCategoryDto);
        Category saved = categoryRepository.save(category);
        log.info("Категория создана с id: {}", saved.getId());
        return categoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        log.info("Обновление категории по id: {}", catId);
        Category category = getCatById(catId);
        if (!category.getName().equals(categoryDto.name()) && categoryRepository.existsByName(categoryDto.name())) {
            throw new ConflictException("Категория с именем '" + categoryDto.name() + "' уже существует");
        }
        category.setName(categoryDto.name());
        Category updated = categoryRepository.save(category);
        log.info("Категория обновлена");
        return categoryMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Удаление категории id: {}", catId);
        Category category = getCatById(catId);
        categoryRepository.delete(category);
        log.info("Категория удалена");
    }

    private Category getCatById(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + catId + " не найдена"));
    }
}
