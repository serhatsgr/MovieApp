package com.serhatsgr.mapper;

import com.serhatsgr.dto.DtoFilm;
import com.serhatsgr.dto.DtoFilmIU;
import com.serhatsgr.entity.Category;
import com.serhatsgr.entity.Film;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FilmMapper {

    public Film toEntity(DtoFilmIU dto, Set<Category> categories) {
        if(dto==null) return null;

        Film film = new Film();
        film.setTitle(dto.getTitle());
        film.setDescription(dto.getDescription());
        film.setReleaseDate(dto.getReleaseDate());
        film.setPosterUrl(dto.getPosterUrl());
        film.setTrailerUrl(dto.getTrailerUrl());
        for (Category category : categories) {
            film.addCategory(category);
        }

        return film;
    }

    public DtoFilm toDto(Film film){
        DtoFilm dto =new DtoFilm();
        dto.setId(film.getId());
        dto.setTitle(film.getTitle());
        dto.setDescription(film.getDescription());
        dto.setReleaseDate(film.getReleaseDate());
        dto.setPosterUrl(film.getPosterUrl());
        dto.setTrailerUrl(film.getTrailerUrl());
        dto.setAverageRating(film.getAverageRating() != null ? film.getAverageRating() : 0.0);
        dto.setRatingCount(film.getRatingCount() != null ? film.getRatingCount() : 0);

        List<String> category=film.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toList());

        dto.setCategorys(category);

        return dto;
    }

    //birden fazla film
    public List<DtoFilm> toDtoList(List<Film> films) {
        return films.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void updateEntity(Film film, DtoFilmIU dto, Set<Category> categories) {
        film.setTitle(dto.getTitle());
        film.setDescription(dto.getDescription());
        film.setReleaseDate(dto.getReleaseDate());
        film.setPosterUrl(dto.getPosterUrl());
        film.setTrailerUrl(dto.getTrailerUrl());
        film.setCategories(categories);
    }




}
