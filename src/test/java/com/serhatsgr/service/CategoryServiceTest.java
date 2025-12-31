package com.serhatsgr.service;

import com.serhatsgr.dto.DtoCategory;
import com.serhatsgr.dto.DtoCategoryIU;
import com.serhatsgr.entity.Category;
import com.serhatsgr.entity.Film;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.mapper.CategoryMapper;
import com.serhatsgr.repository.CategoryRepository;
import com.serhatsgr.repository.FilmRepository;
import com.serhatsgr.service.Impl.CategoryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private FilmRepository filmRepository;
    @Mock private CategoryMapper categoryMapper;

    @InjectMocks private CategoryServiceImpl categoryService;

    // ---------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------
    @Test
    @DisplayName("createCategory → Başarılı senaryo")
    void createCategory_Success() {
        DtoCategoryIU req = new DtoCategoryIU("Action", "Action Movies");

        Category entity = new Category();
        entity.setId(1L);
        entity.setName("Action");

        DtoCategory responseDto = new DtoCategory(1L, "Action", "Action Movies", null);

        given(categoryRepository.existsByNameIgnoreCase("Action")).willReturn(false);
        given(categoryMapper.toEntity(req)).willReturn(entity);
        given(categoryRepository.save(entity)).willReturn(entity);
        given(categoryMapper.toDto(entity)).willReturn(responseDto);

        DtoCategory result = categoryService.createCategory(req);

        assertThat(result.getName()).isEqualTo("Action");
    }

    @Test
    @DisplayName("createCategory → Aynı isimle kategori varsa hata")
    void createCategory_DuplicateName_ThrowsException() {
        DtoCategoryIU req = new DtoCategoryIU("Action", "Desc");

        given(categoryRepository.existsByNameIgnoreCase("Action")).willReturn(true);

        Throwable thrown = catchThrowable(() -> categoryService.createCategory(req));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.DUPLICATE_RESOURCE);
    }

    // ---------------------------------------------------------
    // GET ALL
    // ---------------------------------------------------------
    @Test
    @DisplayName("getAllCategories → Liste başarıyla dönmeli")
    void getAllCategories_Success() {
        Category c1 = new Category(); c1.setId(1L); c1.setName("Action");
        Category c2 = new Category(); c2.setId(2L); c2.setName("Drama");
        List<Category> categories = List.of(c1, c2);

        List<DtoCategory> dtos = List.of(
                new DtoCategory(1L, "Action", "Desc", null),
                new DtoCategory(2L, "Drama", "Desc", null)
        );

        given(categoryRepository.findAll()).willReturn(categories);
        given(categoryMapper.toDtoList(categories)).willReturn(dtos);

        List<DtoCategory> result = categoryService.getAllCategories();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Action");
    }

    // ---------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------
    @Test
    @DisplayName("updateCategory → Başarılı güncelleme")
    void updateCategory_Success() {
        Long id = 1L;
        Category existing = new Category();
        existing.setId(id);
        existing.setName("OldName");

        DtoCategoryIU req = new DtoCategoryIU("NewName", "New Desc");
        DtoCategory responseDto = new DtoCategory(1L, "NewName", "New Desc", null);

        given(categoryRepository.findById(id)).willReturn(Optional.of(existing));
        given(categoryRepository.existsByNameIgnoreCase("NewName")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willReturn(existing);
        given(categoryMapper.toDto(any(Category.class))).willReturn(responseDto);

        DtoCategory updated = categoryService.updateCategory(req, id);

        assertThat(updated.getName()).isEqualTo("NewName");
    }

    @Test
    @DisplayName("updateCategory → Yeni isim başka kategoride varsa hata")
    void updateCategory_DuplicateName_ThrowsException() {
        Long id = 1L;
        Category existing = new Category();
        existing.setId(id);
        existing.setName("OldName");

        DtoCategoryIU req = new DtoCategoryIU("Action", "Desc");

        given(categoryRepository.findById(id)).willReturn(Optional.of(existing));
        given(categoryRepository.existsByNameIgnoreCase("Action")).willReturn(true);

        Throwable thrown = catchThrowable(() -> categoryService.updateCategory(req, id));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.DUPLICATE_RESOURCE);
    }

    @Test
    @DisplayName("updateCategory → ID bulunamazsa NOT_FOUND")
    void updateCategory_NotFound_ThrowsException() {
        given(categoryRepository.findById(1L)).willReturn(Optional.empty());
        DtoCategoryIU req = new DtoCategoryIU("Any", "Desc");

        Throwable thrown = catchThrowable(() -> categoryService.updateCategory(req, 1L));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.NOT_FOUND);
    }

    // ---------------------------------------------------------
    // DELETE (DÜZELTİLEN KISIM)
    // ---------------------------------------------------------
    @Test
    @DisplayName("deleteCategory → Kategori ve bağlı filmler silinmeli")
    void deleteCategory_Success() {
        // Arrange
        Long id = 1L;
        Category category = new Category();
        category.setId(id);
        category.setName("Action");

        Film film = new Film();
        film.setId(100L);
        category.setFilms(new HashSet<>(Collections.singletonList(film)));

        given(categoryRepository.findById(id)).willReturn(Optional.of(category));

        // Act
        String result = categoryService.deleteCategory(id);

        // Assert
        // DÜZELTME: Artık film.save() değil, film.delete() çağrılmalı.
        verify(filmRepository, times(1)).delete(any(Film.class));
        verify(categoryRepository).delete(category);
        assertThat(result).contains("başarıyla silindi");
    }

    @Test
    @DisplayName("deleteCategory → ID bulunamazsa NOT_FOUND")
    void deleteCategory_NotFound_ThrowsException() {
        given(categoryRepository.findById(1L)).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> categoryService.deleteCategory(1L));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.NOT_FOUND);
    }
}