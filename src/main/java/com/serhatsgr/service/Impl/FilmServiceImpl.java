package com.serhatsgr.service.Impl;

import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.mapper.FilmMapper;
import com.serhatsgr.dto.DtoFilm;
import com.serhatsgr.dto.DtoFilmIU;
import com.serhatsgr.entity.Category;
import com.serhatsgr.entity.Film;
import com.serhatsgr.repository.CategoryRepository;
import com.serhatsgr.repository.FilmRepository;
import com.serhatsgr.service.IFilmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FilmServiceImpl implements IFilmService {

    private static final Logger logger = LoggerFactory.getLogger(FilmServiceImpl.class);

    private final FilmRepository filmRepository;
    private final CategoryRepository categoryRepository;
    private final FilmMapper filmMapper;

    public FilmServiceImpl(FilmRepository filmRepository,
                           CategoryRepository categoryRepository,
                           FilmMapper filmMapper) {
        this.filmRepository = filmRepository;
        this.categoryRepository = categoryRepository;
        this.filmMapper = filmMapper;
    }

    @Override
    public DtoFilm addFilm(DtoFilmIU dtoFilmIU) {

        if (dtoFilmIU == null) {
            throw new BaseException(new ErrorMessage(MessageType.BAD_REQUEST, "Film bilgisi boş olamaz"));
        }

        if (dtoFilmIU.getCategoryIds() == null || dtoFilmIU.getCategoryIds().isEmpty()) {
            throw new BaseException(new ErrorMessage(MessageType.VALIDATION_ERROR, "Film en az bir kategoriye sahip olmalı."));
        }

        Set<Long> categoryIds = new HashSet<>(dtoFilmIU.getCategoryIds());
        List<Category> categories = categoryRepository.findAllById(categoryIds);

        if (categories.size() != categoryIds.size()) {
            categoryIds.removeAll(categories.stream().map(Category::getId).toList());
            throw new BaseException(new ErrorMessage(MessageType.NOT_FOUND,
                    "Aşağıdaki kategori ID’leri geçersiz veya bulunamadı: " + categoryIds));
        }

        if (filmRepository.existsByTitle(dtoFilmIU.getTitle())) {
            throw new BaseException(new ErrorMessage(MessageType.DUPLICATE_RESOURCE, "Bu film adı zaten kayıtlı."));
        }



        if (filmRepository.existsByPosterUrl(dtoFilmIU.getPosterUrl())) {
            throw new BaseException(new ErrorMessage(MessageType.DUPLICATE_RESOURCE, "Bu poster URL başka bir film tarafından kullanılıyor."));
        }

        if (filmRepository.existsByTrailerUrl(dtoFilmIU.getTrailerUrl())) {
            throw new BaseException(new ErrorMessage(MessageType.DUPLICATE_RESOURCE, "Bu fragman URL başka bir film tarafından kullanılıyor."));
        }

        Film film = filmMapper.toEntity(dtoFilmIU, new HashSet<>(categories));
        Film savedFilm = filmRepository.save(film);
        return filmMapper.toDto(savedFilm);
    }

    @Override
    public List<DtoFilm> getAllFilms() {
        List<Film> films = filmRepository.findAll();
        if (films.isEmpty()) {
            throw new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Hiç film bulunamadı."));
        }
        return filmMapper.toDtoList(films);
    }

    @Override
    public DtoFilm getFilmById(Long id) {
        Film dbFilm = filmRepository.findById(id)
                .orElseThrow(() -> new BaseException(
                        new ErrorMessage(MessageType.NOT_FOUND, id + " ID’li film bulunamadı.")
                ));
        return filmMapper.toDto(dbFilm);
    }

    @Override
    public String deleteFilmById(Long id) {
        Film film = filmRepository.findById(id)
                .orElseThrow(() -> new BaseException(
                        new ErrorMessage(MessageType.NOT_FOUND, "Silinecek film bulunamadı: " + id)
                ));
        filmRepository.delete(film);
        return film.getTitle() + " filmi başarıyla silindi";
    }


    @Override
    public DtoFilm updateFilm(DtoFilmIU dtoFilmIU, Long id) {
        if (dtoFilmIU == null) {
            throw new BaseException(new ErrorMessage(MessageType.BAD_REQUEST, "Film bilgisi boş olamaz"));
        }

        if (id == null) {
            throw new BaseException(new ErrorMessage(MessageType.BAD_REQUEST, "Film ID boş olamaz"));
        }

        // Güncellenecek film var mı kontrolü
        Film existingFilm = filmRepository.findById(id)
                .orElseThrow(() -> new BaseException(
                        new ErrorMessage(MessageType.NOT_FOUND, "Güncellenecek film bulunamadı: " + id)
                ));

        // Kategori kontrolü
        if (dtoFilmIU.getCategoryIds() == null || dtoFilmIU.getCategoryIds().isEmpty()) {
            throw new BaseException(new ErrorMessage(MessageType.VALIDATION_ERROR, "Film en az bir kategoriye sahip olmalı."));
        }

        Set<Long> categoryIds = new HashSet<>(dtoFilmIU.getCategoryIds());
        List<Category> categories = categoryRepository.findAllById(categoryIds);

        if (categories.size() != categoryIds.size()) {
            categoryIds.removeAll(categories.stream().map(Category::getId).toList());
            throw new BaseException(new ErrorMessage(MessageType.NOT_FOUND,
                    "Aşağıdaki kategori ID’leri geçersiz veya bulunamadı: " + categoryIds));
        }

        // 🎯 Duplicate kontrolleri (kendi kaydı hariç)
        if (filmRepository.existsByTitleAndIdNot(dtoFilmIU.getTitle(), id)) {
            throw new BaseException(new ErrorMessage(MessageType.DUPLICATE_RESOURCE, "Bu film adı zaten kayıtlı."));
        }

        if (filmRepository.existsByPosterUrlAndIdNot(dtoFilmIU.getPosterUrl(), id)) {
            throw new BaseException(new ErrorMessage(MessageType.DUPLICATE_RESOURCE, "Bu poster URL başka bir film tarafından kullanılıyor."));
        }

        if (filmRepository.existsByTrailerUrlAndIdNot(dtoFilmIU.getTrailerUrl(), id)) {
            throw new BaseException(new ErrorMessage(MessageType.DUPLICATE_RESOURCE, "Bu fragman URL başka bir film tarafından kullanılıyor."));
        }

        // Güncelleme işlemi
        filmMapper.updateEntity(existingFilm, dtoFilmIU, new HashSet<>(categories));
        Film updatedFilm = filmRepository.save(existingFilm);

        return filmMapper.toDto(updatedFilm);
    }


}

