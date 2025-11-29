package com.serhatsgr.repository;

import com.serhatsgr.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findAllByUserId(Long userId);
    Optional<Favorite> findByUserIdAndFilmId(Long userId, Long filmId);
    boolean existsByUserIdAndFilmId(Long userId, Long filmId);
}