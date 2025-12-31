package com.serhatsgr.mapper;

import com.serhatsgr.dto.DtoFilm;
import com.serhatsgr.dto.DtoFilmIU;
import com.serhatsgr.entity.Category;
import com.serhatsgr.entity.Film;
import com.serhatsgr.entity.ListingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FilmMapperTest {

    private final FilmMapper filmMapper = new FilmMapper();

    // ----------------------------------------------------------
    // toEntity
    // ----------------------------------------------------------
    @Test
    @DisplayName("toEntity -> DTO ve Kategori Seti ile Entity oluşturulmalı")
    void toEntity_Success() {
        // Given
        DtoFilmIU dto = new DtoFilmIU();
        dto.setTitle("Matrix");
        dto.setDescription("The One");
        dto.setReleaseDate(LocalDate.of(1999, 3, 31));
        dto.setListingType(ListingType.VISION);

        Category c1 = new Category(); c1.setName("Sci-Fi");
        Set<Category> categories = Set.of(c1);

        // When
        Film film = filmMapper.toEntity(dto, categories);

        // Then
        assertThat(film.getTitle()).isEqualTo("Matrix");
        assertThat(film.getReleaseDate()).isEqualTo(LocalDate.of(1999, 3, 31));
        assertThat(film.getCategories()).contains(c1); // İlişki kurulmuş mu?
    }

    @Test
    @DisplayName("toEntity -> DTO null ise null dönmeli")
    void toEntity_NullDto() {
        assertThat(filmMapper.toEntity(null, new HashSet<>())).isNull();
    }

    // ----------------------------------------------------------
    // toDto
    // ----------------------------------------------------------
    @Test
    @DisplayName("toDto -> Entity verileri doğru taşınmalı, Rating null ise 0 dönmeli")
    void toDto_Success() {
        // Given
        Film film = new Film();
        film.setId(1L);
        film.setTitle("Inception");
        film.setAverageRating(null); // Null rating testi
        film.setRatingCount(null);   // Null count testi

        Category c1 = new Category(); c1.setName("Thriller");
        film.setCategories(Set.of(c1));

        // When
        DtoFilm dto = filmMapper.toDto(film);

        // Then
        assertThat(dto.getTitle()).isEqualTo("Inception");
        assertThat(dto.getAverageRating()).isEqualTo(0.0); // Null check testi
        assertThat(dto.getRatingCount()).isEqualTo(0);
        assertThat(dto.getCategorys()).containsExactly("Thriller"); // İsim listesi kontrolü
    }

    @Test
    @DisplayName("toDto -> Rating varsa o değer dönmeli")
    void toDto_WithRating() {
        Film film = new Film();
        film.setAverageRating(8.8);
        film.setRatingCount(100);
        film.setCategories(new HashSet<>());

        DtoFilm dto = filmMapper.toDto(film);

        assertThat(dto.getAverageRating()).isEqualTo(8.8);
        assertThat(dto.getRatingCount()).isEqualTo(100);
    }

    // ----------------------------------------------------------
    // updateEntity
    // ----------------------------------------------------------
    @Test
    @DisplayName("updateEntity -> Mevcut film güncellenmeli")
    void updateEntity_Success() {
        // Given (Eski Hali)
        Film film = new Film();
        film.setTitle("Eski Başlık");
        film.setDescription("Eski Açıklama");
        film.setCategories(new HashSet<>());

        // Yeni Veriler
        DtoFilmIU updateDto = new DtoFilmIU();
        updateDto.setTitle("Yeni Başlık");
        updateDto.setDescription("Yeni Açıklama");

        Category newCat = new Category(); newCat.setName("Yeni Kategori");
        Set<Category> newCategories = Set.of(newCat);

        // When
        filmMapper.updateEntity(film, updateDto, newCategories);

        // Then
        assertThat(film.getTitle()).isEqualTo("Yeni Başlık");
        assertThat(film.getDescription()).isEqualTo("Yeni Açıklama");
        assertThat(film.getCategories()).containsExactly(newCat);
    }

    // ----------------------------------------------------------
    // toDtoList
    // ----------------------------------------------------------
    @Test
    @DisplayName("toDtoList -> Liste dönüşümü")
    void toDtoList_Success() {
        Film f1 = new Film(); f1.setTitle("F1"); f1.setCategories(new HashSet<>());
        Film f2 = new Film(); f2.setTitle("F2"); f2.setCategories(new HashSet<>());

        List<DtoFilm> dtos = filmMapper.toDtoList(List.of(f1, f2));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getTitle()).isEqualTo("F1");
    }
}