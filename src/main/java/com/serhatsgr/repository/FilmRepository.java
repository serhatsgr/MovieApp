package com.serhatsgr.repository;

import com.serhatsgr.entity.Film;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilmRepository extends JpaRepository<Film, Long> {
    boolean existsByPosterUrl(String posterUrl);

    boolean existsByTrailerUrl(String trailerUrl);

    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, Long id);

    boolean existsByPosterUrlAndIdNot(String posterUrl, Long id);

    boolean existsByTrailerUrlAndIdNot(String trailerUrl, Long id);

}
