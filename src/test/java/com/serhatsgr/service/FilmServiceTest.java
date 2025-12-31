package com.serhatsgr.service;

import com.serhatsgr.dto.DtoFilm;
import com.serhatsgr.dto.DtoFilmIU;
import com.serhatsgr.entity.Category;
import com.serhatsgr.entity.Film;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.mapper.FilmMapper;
import com.serhatsgr.repository.CategoryRepository;
import com.serhatsgr.repository.FilmRepository;
import com.serhatsgr.service.Impl.FilmServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilmServiceTest {

    @Mock private FilmRepository filmRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private FilmMapper filmMapper;

    @InjectMocks private FilmServiceImpl filmService;

    @Test
    @DisplayName("addFilm -> Kategori bulunamazsa NOT_FOUND hatası")
    void addFilm_CategoryNotFound() {
        DtoFilmIU req = new DtoFilmIU();
        req.setCategoryIds(List.of(1L, 2L));

        given(categoryRepository.findAllById(anySet()))
                .willReturn(List.of(new Category())); // Eksik kategori dönüyor

        Throwable thrown = catchThrowable(() -> filmService.addFilm(req));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.NOT_FOUND);
    }

    @Test
    @DisplayName("addFilm -> Başarılı")
    void addFilm_Success() {
        DtoFilmIU req = new DtoFilmIU();
        req.setTitle("Test Movie");
        req.setCategoryIds(List.of(1L));

        Category cat = new Category();
        cat.setId(1L);

        Film film = new Film();
        film.setId(10L);
        film.setTitle("Test Movie");

        DtoFilm dto = new DtoFilm(); // mapper dönüş değeri

        given(categoryRepository.findAllById(anySet())).willReturn(List.of(cat));
        given(filmMapper.toEntity(any(DtoFilmIU.class), anySet())).willReturn(film);
        given(filmRepository.save(film)).willReturn(film);
        given(filmMapper.toDto(film)).willReturn(dto);

        DtoFilm res = filmService.addFilm(req);

        assertThat(res).isNotNull();
    }

    @Test
    @DisplayName("getAllFilms -> Liste döner")
    void getAllFilms_Success() {
        given(filmRepository.findAll()).willReturn(List.of(new Film()));
        given(filmMapper.toDtoList(any())).willReturn(List.of(new DtoFilm()));

        List<DtoFilm> res = filmService.getAllFilms(null);

        assertThat(res).hasSize(1);
    }

    @Test
    @DisplayName("getFilmById -> Film bulunamazsa NOT_FOUND hatası")
    void getFilmById_NotFound() {
        given(filmRepository.findById(1L)).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> filmService.getFilmById(1L));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.NOT_FOUND);
    }

    @Test
    @DisplayName("getFilmById -> Başarılı")
    void getFilmById_Success() {
        Film film = new Film();
        film.setId(1L);

        given(filmRepository.findById(1L)).willReturn(Optional.of(film));
        given(filmMapper.toDto(film)).willReturn(new DtoFilm());

        DtoFilm result = filmService.getFilmById(1L);

        assertThat(result).isNotNull();
    }

    // --- DÜZELTİLEN TESTLER ---

    @Test
    @DisplayName("deleteFilm -> Film bulunamazsa NOT_FOUND")
    void deleteFilm_NotFound() {
        given(filmRepository.findById(99L)).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> filmService.deleteFilmById(99L));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.NOT_FOUND);
    }

    @Test
    @DisplayName("deleteFilm -> Başarılı")
    void deleteFilm_Success() {
        Film film = new Film();
        film.setId(5L);
        film.setTitle("Silinecek Film");
        given(filmRepository.findById(5L)).willReturn(Optional.of(film));
        filmService.deleteFilmById(5L);
        verify(filmRepository).delete(film);
    }
}