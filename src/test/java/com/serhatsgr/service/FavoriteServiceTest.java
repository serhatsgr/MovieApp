package com.serhatsgr.service;

import com.serhatsgr.dto.DtoFilm;
import com.serhatsgr.entity.Favorite;
import com.serhatsgr.entity.Film;
import com.serhatsgr.entity.User;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.mapper.FilmMapper;
import com.serhatsgr.repository.FavoriteRepository;
import com.serhatsgr.repository.FilmRepository;
import com.serhatsgr.repository.UserRepository;
import com.serhatsgr.service.Impl.FavoriteService;
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
class FavoriteServiceTest {

    @Mock private FavoriteRepository favoriteRepository;
    @Mock private FilmRepository filmRepository;
    @Mock private UserRepository userRepository;
    @Mock private FilmMapper filmMapper;

    @InjectMocks private FavoriteService favoriteService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        // Security Context Mocking
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        given(securityContext.getAuthentication()).willReturn(auth);
        given(auth.getName()).willReturn("testUser");
        SecurityContextHolder.setContext(securityContext);

        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("testUser");
    }

    @Test
    @DisplayName("addFavorite -> Başarılı ekleme")
    void addFavorite_Success() {
        // Given
        Long filmId = 10L;
        Film film = new Film(); film.setId(filmId);

        given(userRepository.findByUsername("testUser")).willReturn(Optional.of(currentUser));
        given(favoriteRepository.existsByUserIdAndFilmId(currentUser.getId(), filmId)).willReturn(false);
        given(filmRepository.findById(filmId)).willReturn(Optional.of(film));

        // When
        favoriteService.addFavorite(filmId);

        // Then
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("addFavorite -> Zaten ekliyse işlem yapma")
    void addFavorite_AlreadyExists() {
        // Given
        given(userRepository.findByUsername("testUser")).willReturn(Optional.of(currentUser));
        given(favoriteRepository.existsByUserIdAndFilmId(currentUser.getId(), 10L)).willReturn(true);

        // When
        favoriteService.addFavorite(10L);

        // Then
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("removeFavorite -> Başarılı silme")
    void removeFavorite_Success() {
        // Given
        Long filmId = 10L;
        Favorite fav = new Favorite();

        given(userRepository.findByUsername("testUser")).willReturn(Optional.of(currentUser));
        given(favoriteRepository.findByUserIdAndFilmId(currentUser.getId(), filmId)).willReturn(Optional.of(fav));

        // When
        favoriteService.removeFavorite(filmId);

        // Then
        verify(favoriteRepository).delete(fav);
    }

    @Test
    @DisplayName("removeFavorite -> Favori bulunamazsa hata")
    void removeFavorite_NotFound() {
        // Given
        given(userRepository.findByUsername("testUser")).willReturn(Optional.of(currentUser));
        given(favoriteRepository.findByUserIdAndFilmId(currentUser.getId(), 10L)).willReturn(Optional.empty());

        // When
        Throwable thrown = catchThrowable(() -> favoriteService.removeFavorite(10L));

        // Then
        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("getMyFavorites -> Liste dönmeli")
    void getMyFavorites_Success() {
        // Given
        Favorite fav = new Favorite();
        Film film = new Film();
        fav.setFilm(film);

        given(userRepository.findByUsername("testUser")).willReturn(Optional.of(currentUser));
        given(favoriteRepository.findAllByUserId(currentUser.getId())).willReturn(List.of(fav));
        given(filmMapper.toDto(film)).willReturn(new DtoFilm());

        // When
        List<DtoFilm> result = favoriteService.getMyFavorites();

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("addFavorite -> Kullanıcı bulunamazsa USER_NOT_FOUND hatası")
    void addFavorite_UserNotFound() {
        given(userRepository.findByUsername("testUser")).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> favoriteService.addFavorite(10L));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("removeFavorite -> Kullanıcı bulunamazsa USER_NOT_FOUND hatası")
    void removeFavorite_UserNotFound() {
        given(userRepository.findByUsername("testUser")).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> favoriteService.removeFavorite(10L));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }


    @Test
    @DisplayName("getMyFavorites -> Kullanıcı bulunamazsa USER_NOT_FOUND hatası")
    void getMyFavorites_UserNotFound() {
        given(userRepository.findByUsername("testUser")).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(favoriteService::getMyFavorites);

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }




}