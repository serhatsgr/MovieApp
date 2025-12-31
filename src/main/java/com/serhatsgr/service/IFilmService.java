package com.serhatsgr.service;

import com.serhatsgr.dto.DtoFilm;
import com.serhatsgr.dto.DtoFilmIU;
import com.serhatsgr.entity.ListingType;

import java.util.List;

public interface IFilmService {

    public DtoFilm addFilm(DtoFilmIU dtoFilmIU);

    List<DtoFilm> getAllFilms(ListingType type);

    public DtoFilm getFilmById(Long id);

    public String deleteFilmById(Long id);

    public DtoFilm updateFilm(DtoFilmIU dtoFilm, Long id);

    public List<DtoFilm> searchFilms(String title);

}
