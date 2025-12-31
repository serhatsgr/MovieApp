package com.serhatsgr.mapper;

import com.serhatsgr.dto.DtoCategory;
import com.serhatsgr.dto.DtoCategoryIU;
import com.serhatsgr.entity.Category;
import com.serhatsgr.entity.Film;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    private final CategoryMapper categoryMapper = new CategoryMapper();

    // ----------------------------------------------------------
    // toEntity
    // ----------------------------------------------------------
    @Test
    @DisplayName("toEntity -> DTO'dan Entity'ye dönüşüm başarılı olmalı")
    void toEntity_Success() {
        // Given
        DtoCategoryIU dto = new DtoCategoryIU("Bilim Kurgu", "Uzay ve gelecek temalı filmler");

        // When
        Category category = categoryMapper.toEntity(dto);

        // Then
        assertThat(category.getName()).isEqualTo("Bilim Kurgu");
        assertThat(category.getDescription()).isEqualTo("Uzay ve gelecek temalı filmler");
    }

    // ----------------------------------------------------------
    // toDto
    // ----------------------------------------------------------
    @Test
    @DisplayName("toDto -> Entity'den DTO'ya dönüşüm (Filmlerle birlikte) başarılı olmalı")
    void toDto_Success() {
        // Given
        Category category = new Category();
        category.setId(1L);
        category.setName("Aksiyon");
        category.setDescription("Patlamalı filmler");

        // Film nesnelerini oluştur
        Film f1 = new Film(); f1.setId(101L); f1.setTitle("Hızlı ve Öfkeli"); f1.setPosterUrl("url1");
        Film f2 = new Film(); f2.setId(102L); f2.setTitle("Görevimiz Tehlike"); f2.setPosterUrl("url2");

        // List yerine Set kullanıyoruz (Entity yapısına uygun)
        Set<Film> films = new HashSet<>();
        films.add(f1);
        films.add(f2);

        category.setFilms(films);

        // When
        DtoCategory dto = categoryMapper.toDto(category);

        // Then
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Aksiyon");

        // Film özetleri kontrolü
        assertThat(dto.getFilmSummaries()).hasSize(2);
        assertThat(dto.getFilmSummaries())
                .anyMatch(summary -> summary.getTitle().equals("Hızlı ve Öfkeli"))
                .anyMatch(summary -> summary.getTitle().equals("Görevimiz Tehlike"));
    }

    @Test
    @DisplayName("toDto -> Film listesi boşsa hata vermemeli, boş liste dönmeli")
    void toDto_NoFilms() {
        Category category = new Category();
        category.setName("Dram");
        // Boş Set veriyoruz
        category.setFilms(new HashSet<>());

        DtoCategory dto = categoryMapper.toDto(category);

        assertThat(dto.getName()).isEqualTo("Dram");
        assertThat(dto.getFilmSummaries()).isEmpty();
    }

    // ----------------------------------------------------------
    // toDtoList
    // ----------------------------------------------------------
    @Test
    @DisplayName("toDtoList -> Liste dönüşümü başarılı olmalı")
    void toDtoList_Success() {
        // Veri Hazırlığı
        Category c1 = new Category(); c1.setName("Komedi"); c1.setFilms(new HashSet<>());
        Category c2 = new Category(); c2.setName("Korku"); c2.setFilms(new HashSet<>());

        // İşlem
        List<DtoCategory> dtos = categoryMapper.toDtoList(List.of(c1, c2));

        // Kontrol
        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getName()).isEqualTo("Komedi");
    }

    @Test
    @DisplayName("toDtoList -> Null veya boş liste gelirse boş liste dönmeli")
    void toDtoList_NullCheck() {
        assertThat(categoryMapper.toDtoList(null)).isEmpty();
        assertThat(categoryMapper.toDtoList(new ArrayList<>())).isEmpty();
    }
}