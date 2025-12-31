package com.serhatsgr.service;

import com.serhatsgr.dto.RatingRequest;
import com.serhatsgr.dto.UserRatingResponse;
import com.serhatsgr.entity.Film;
import com.serhatsgr.entity.Rating;
import com.serhatsgr.entity.User;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.repository.FilmRepository;
import com.serhatsgr.repository.RatingRepository;
import com.serhatsgr.repository.UserRepository;
import com.serhatsgr.service.Impl.RatingService;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock private RatingRepository ratingRepository;
    @Mock private FilmRepository filmRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private RatingService ratingService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        given(securityContext.getAuthentication()).willReturn(auth);
        given(auth.getName()).willReturn("user");
        SecurityContextHolder.setContext(securityContext);

        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("user");
    }

    // ------------------------------------------------------------
    // CREATE / UPDATE
    // ------------------------------------------------------------

    @Test
    @DisplayName("createOrUpdateRating -> Yeni rating kaydedilmeli ve film istatistikleri güncellenmeli")
    void createOrUpdateRating_NewRating() {
        Long filmId = 10L;
        RatingRequest req = new RatingRequest(5);
        Film film = new Film(); film.setId(filmId);

        given(userRepository.findByUsername("user")).willReturn(Optional.of(currentUser));
        given(filmRepository.findById(filmId)).willReturn(Optional.of(film));
        given(ratingRepository.findByFilmIdAndUserId(filmId, 1L)).willReturn(Optional.empty());
        given(ratingRepository.getAverageRating(filmId)).willReturn(4.5);
        given(ratingRepository.getRatingCount(filmId)).willReturn(10);

        ratingService.createOrUpdateRating(filmId, req);

        verify(ratingRepository).save(any(Rating.class));
        verify(filmRepository).save(film);

        assertThat(film.getAverageRating()).isEqualTo(4.5);
        assertThat(film.getRatingCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("createOrUpdateRating -> Var olan rating güncellenmeli")
    void createOrUpdateRating_UpdateExisting() {
        Long filmId = 10L;
        RatingRequest req = new RatingRequest(3);

        Film film = new Film(); film.setId(filmId);
        Rating rating = Rating.builder().user(currentUser).film(film).score(5).build();

        given(userRepository.findByUsername("user")).willReturn(Optional.of(currentUser));
        given(filmRepository.findById(filmId)).willReturn(Optional.of(film));
        given(ratingRepository.findByFilmIdAndUserId(filmId, 1L)).willReturn(Optional.of(rating));
        given(ratingRepository.getAverageRating(filmId)).willReturn(3.0);
        given(ratingRepository.getRatingCount(filmId)).willReturn(1);

        ratingService.createOrUpdateRating(filmId, req);

        assertThat(rating.getScore()).isEqualTo(3);
        verify(ratingRepository).save(rating);
    }

    @Test
    @DisplayName("createOrUpdateRating -> Kullanıcı bulunamazsa hata fırlatmalı")
    void createOrUpdateRating_UserNotFound() {
        given(userRepository.findByUsername("user")).willReturn(Optional.empty());

        Throwable ex = catchThrowable(() ->
                ratingService.createOrUpdateRating(5L, new RatingRequest(5))
        );

        assertThat(ex)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("createOrUpdateRating -> Film bulunamazsa hata fırlatmalı")
    void createOrUpdateRating_FilmNotFound() {
        given(userRepository.findByUsername("user")).willReturn(Optional.of(currentUser));
        given(filmRepository.findById(10L)).willReturn(Optional.empty());

        Throwable ex = catchThrowable(() ->
                ratingService.createOrUpdateRating(10L, new RatingRequest(5))
        );

        assertThat(ex)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }

    // ------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------

    @Test
    @DisplayName("deleteRating -> Silme başarılı olmalı")
    void deleteRating_Success() {
        Long filmId = 10L;
        Film film = new Film(); film.setId(filmId);
        Rating rating = new Rating();

        given(userRepository.findByUsername("user")).willReturn(Optional.of(currentUser));
        given(ratingRepository.findByFilmIdAndUserId(filmId, 1L)).willReturn(Optional.of(rating));
        given(filmRepository.findById(filmId)).willReturn(Optional.of(film));

        ratingService.deleteRating(filmId);

        verify(ratingRepository).delete(rating);
        verify(filmRepository).save(film);
    }

    @Test
    @DisplayName("deleteRating -> Rating bulunamazsa hata fırlatmalı")
    void deleteRating_NotFound() {
        given(userRepository.findByUsername("user")).willReturn(Optional.of(currentUser));
        given(ratingRepository.findByFilmIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        Throwable ex = catchThrowable(() -> ratingService.deleteRating(10L));

        assertThat(ex)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }

    // ------------------------------------------------------------
    // GET USER RATING
    // ------------------------------------------------------------

    @Test
    @DisplayName("getUserRating -> Kullanıcı puanı varsa dönmeli")
    void getUserRating_Success() {
        Rating r = new Rating();
        r.setScore(4);

        given(userRepository.findByUsername("user")).willReturn(Optional.of(currentUser));
        given(ratingRepository.findByFilmIdAndUserId(10L, 1L)).willReturn(Optional.of(r));

        UserRatingResponse res = ratingService.getUserRating(10L);

        assertThat(res.score()).isEqualTo(4);
    }

    @Test
    @DisplayName("getUserRating -> Kullanıcı hiç puan vermemişse 0 dönmeli")
    void getUserRating_NoRating() {
        given(userRepository.findByUsername("user")).willReturn(Optional.of(currentUser));
        given(ratingRepository.findByFilmIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        UserRatingResponse res = ratingService.getUserRating(10L);

        assertThat(res.score()).isEqualTo(0);
    }

    @Test
    @DisplayName("getUserRating -> Kullanıcı bulunamazsa hata fırlatmalı")
    void getUserRating_UserNotFound() {
        given(userRepository.findByUsername("user")).willReturn(Optional.empty());

        Throwable ex = catchThrowable(() -> ratingService.getUserRating(10L));

        assertThat(ex)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }
}
