package com.serhatsgr.service;

import com.serhatsgr.dto.DtoFilm;
import com.serhatsgr.dto.DtoFilmIU;

import java.util.List;

public interface IFilmService {

    public DtoFilm addFilm(DtoFilmIU dtoFilmIU);

    public List<DtoFilm> getAllFilms();

    public DtoFilm getFilmById(Long id);

    public String deleteFilmById(Long id);

    public DtoFilm updateFilm(DtoFilmIU dtoFilm, Long id);

}
