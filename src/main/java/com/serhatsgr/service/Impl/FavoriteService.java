package com.serhatsgr.service.Impl;

import com.serhatsgr.dto.DtoFilm;
import com.serhatsgr.entity.*;
import com.serhatsgr.exception.*;
import com.serhatsgr.mapper.FilmMapper;
import com.serhatsgr.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final FilmRepository filmRepository;
    private final UserRepository userRepository;
    private final FilmMapper filmMapper;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı")));
    }

    @Transactional
    public void addFavorite(Long filmId) {
        User user = getCurrentUser();
        if (favoriteRepository.existsByUserIdAndFilmId(user.getId(), filmId)) return; // Zaten ekli

        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Film bulunamadı")));

        Favorite favorite = Favorite.builder().user(user).film(film).build();
        favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(Long filmId) {
        User user = getCurrentUser();
        Favorite fav = favoriteRepository.findByUserIdAndFilmId(user.getId(), filmId)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Favori bulunamadı")));
        favoriteRepository.delete(fav);
    }

    public List<DtoFilm> getMyFavorites() {
        User user = getCurrentUser();
        return favoriteRepository.findAllByUserId(user.getId()).stream()
                .map(fav -> filmMapper.toDto(fav.getFilm()))
                .collect(Collectors.toList());
    }
}