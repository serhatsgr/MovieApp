package com.serhatsgr.service.Impl;

import com.serhatsgr.dto.RatingRequest;
import com.serhatsgr.dto.UserRatingResponse;
import com.serhatsgr.entity.Film;
import com.serhatsgr.entity.Rating;
import com.serhatsgr.entity.User;
import com.serhatsgr.exception.*;
import com.serhatsgr.repository.FilmRepository;
import com.serhatsgr.repository.RatingRepository;
import com.serhatsgr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final FilmRepository filmRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createOrUpdateRating(Long filmId, RatingRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı")));

        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Film bulunamadı")));

        Rating rating = ratingRepository.findByFilmIdAndUserId(filmId, user.getId())
                .orElse(Rating.builder().user(user).film(film).build());

        rating.setScore(request.score());
        ratingRepository.save(rating);

        // Denormalize alanları güncelleme
        updateFilmRatingStats(film);
    }

    @Transactional
    public void deleteRating(Long filmId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı")));

        Rating rating = ratingRepository.findByFilmIdAndUserId(filmId, user.getId())
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Oylama bulunamadı")));

        ratingRepository.delete(rating);
        Film film = filmRepository.findById(filmId).orElseThrow();
        updateFilmRatingStats(film);
    }

    public UserRatingResponse getUserRating(Long filmId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı")));

        return ratingRepository.findByFilmIdAndUserId(filmId, user.getId())
                .map(r -> new UserRatingResponse(r.getScore()))
                .orElse(new UserRatingResponse(0));
    }

    private void updateFilmRatingStats(Film film) {
        Double avg = ratingRepository.getAverageRating(film.getId());
        Integer count = ratingRepository.getRatingCount(film.getId());

        // 1 ondalık basamağa yuvarla
        double roundedAvg = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;

        film.setAverageRating(roundedAvg);
        film.setRatingCount(count != null ? count : 0);
        filmRepository.save(film);
    }
}