package com.serhatsgr.service;

import com.serhatsgr.dto.DtoFilm;
import com.serhatsgr.entity.Film;
import com.serhatsgr.entity.User;
import com.serhatsgr.entity.Watched;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.mapper.FilmMapper;
import com.serhatsgr.repository.FilmRepository;
import com.serhatsgr.repository.UserRepository;
import com.serhatsgr.repository.WatchedRepository;
import com.serhatsgr.service.Impl.WatchedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchedServiceTest {

    @Mock private WatchedRepository watchedRepository;
    @Mock private FilmRepository filmRepository;
    @Mock private UserRepository userRepository;
    @Mock private FilmMapper filmMapper;

    @InjectMocks private WatchedService watchedService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        Authentication auth = mock(Authentication.class);
        SecurityContext sec = mock(SecurityContext.class);

        given(sec.getAuthentication()).willReturn(auth);
        given(auth.getName()).willReturn("user");
        SecurityContextHolder.setContext(sec);

        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("user");
    }

    @Test
    @DisplayName("markAsWatched -> Başarılı işaretleme")
    void markAsWatched_Success() {
        Long filmId = 5L;
        Film film = new Film(); film.setId(filmId);

        given(userRepository.findByUsername("user")).willReturn(Optional.of(currentUser));
        given(watchedRepository.existsByUserIdAndFilmId(1L, 5L)).willReturn(false);
        given(filmRepository.findById(5L)).willReturn(Optional.of(film));

        watchedService.markAsWatched(5L);

        verify(watchedRepository).save(any(Watched.class));
    }

    @Test
    @DisplayName("markAsWatched -> Kullanıcı bulunamazsa hata fırlatmalı")
    void markAsWatched_UserNotFound() {
        given(userRepository.findByUsername("user")).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> watchedService.markAsWatched(5L));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("markAsWatched -> Film bulunamazsa hata fırlatmalı")
    void markAsWatched_FilmNotFound() {
        given(userRepository.findByUsername("user")).willReturn(Optional.of(currentUser));
        given(watchedRepository.existsByUserIdAndFilmId(1L, 5L)).willReturn(false);
        given(filmRepository.findById(5L)).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> watchedService.markAsWatched(5L));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("unmarkWatched -> Başarılı çıkarma")
    void unmarkWatched_Success() {
        Watched watched = new Watched();

        given(userRepository.findByUsername("user")).willReturn(Optional.of(currentUser));
        given(watchedRepository.findByUserIdAndFilmId(1L, 5L)).willReturn(Optional.of(watched));

        watchedService.unmarkWatched(5L);

        verify(watchedRepository).delete(watched);
    }

    @Test
    @DisplayName("unmarkWatched -> Kayıt bulunamazsa hata")
    void unmarkWatched_NotFound() {
        given(userRepository.findByUsername("user")).willReturn(Optional.of(currentUser));
        given(watchedRepository.findByUserIdAndFilmId(1L, 5L)).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> watchedService.unmarkWatched(5L));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("getMyWatchedList -> Liste döner")
    void getMyWatchedList_Success() {
        Watched w = new Watched(); w.setFilm(new Film());

        given(userRepository.findByUsername("user")).willReturn(Optional.of(currentUser));
        given(watchedRepository.findAllByUserId(1L)).willReturn(List.of(w));
        given(filmMapper.toDto(any())).willReturn(new DtoFilm());

        List<DtoFilm> result = watchedService.getMyWatchedList();

        assertThat(result).hasSize(1);
    }
}
