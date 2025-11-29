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
public class WatchedService {

    private final WatchedRepository watchedRepository;
    private final FilmRepository filmRepository;
    private final UserRepository userRepository;
    private final FilmMapper filmMapper;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı")));
    }

    @Transactional
    public void markAsWatched(Long filmId) {
        User user = getCurrentUser();
        if (watchedRepository.existsByUserIdAndFilmId(user.getId(), filmId)) return;

        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Film bulunamadı")));

        Watched watched = Watched.builder().user(user).film(film).build();
        watchedRepository.save(watched);
    }

    @Transactional
    public void unmarkWatched(Long filmId) {
        User user = getCurrentUser();
        Watched watched = watchedRepository.findByUserIdAndFilmId(user.getId(), filmId)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kayıt bulunamadı")));
        watchedRepository.delete(watched);
    }

    public List<DtoFilm> getMyWatchedList() {
        User user = getCurrentUser();
        return watchedRepository.findAllByUserId(user.getId()).stream()
                .map(w -> filmMapper.toDto(w.getFilm()))
                .collect(Collectors.toList());
    }
}