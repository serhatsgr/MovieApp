package com.serhatsgr.repository;

import com.serhatsgr.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByFilmIdAndUserId(Long filmId, Long userId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.film.id = :filmId")
    Double getAverageRating(Long filmId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.film.id = :filmId")
    Integer getRatingCount(Long filmId);
}

