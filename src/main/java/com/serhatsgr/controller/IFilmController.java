package com.serhatsgr.controller;

import com.serhatsgr.dto.DtoFilm;
import com.serhatsgr.dto.DtoFilmIU;
import com.serhatsgr.dto.ApiSuccess;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface IFilmController {

    ResponseEntity<ApiSuccess<DtoFilm>> createFilm(DtoFilmIU dto);

    ResponseEntity<ApiSuccess<List<DtoFilm>>> getAllFilms();

    ResponseEntity<ApiSuccess<DtoFilm>> getFilmById(Long id);

    ResponseEntity<ApiSuccess<String>> deleteFilmById(Long id);

    ResponseEntity<ApiSuccess<DtoFilm>> updateFilm(DtoFilmIU dtoFilm, Long id);
}
