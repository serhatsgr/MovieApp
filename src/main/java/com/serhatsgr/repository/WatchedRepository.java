package com.serhatsgr.repository;

import com.serhatsgr.entity.Watched;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WatchedRepository extends JpaRepository<Watched, Long> {
    List<Watched> findAllByUserId(Long userId);
    Optional<Watched> findByUserIdAndFilmId(Long userId, Long filmId);
    boolean existsByUserIdAndFilmId(Long userId, Long filmId);
}