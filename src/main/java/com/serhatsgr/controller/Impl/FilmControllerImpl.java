package com.serhatsgr.controller.Impl;

import com.serhatsgr.controller.IFilmController;
import com.serhatsgr.dto.ApiSuccess;
import com.serhatsgr.dto.DtoFilm;
import com.serhatsgr.dto.DtoFilmIU;
import com.serhatsgr.entity.ListingType;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.service.IFilmService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest/api/film")
public class FilmControllerImpl implements IFilmController {

    private final IFilmService filmService;

    public FilmControllerImpl(IFilmService filmService) {
        this.filmService = filmService;
    }

    // --- CREATE ---
    @PostMapping(path = "/save")
    @Override
    public ResponseEntity<ApiSuccess<DtoFilm>> createFilm(@Valid @RequestBody DtoFilmIU dto) {
        try {
            DtoFilm createdFilm = filmService.addFilm(dto);
            return ResponseEntity.ok(ApiSuccess.of("Film başarıyla oluşturuldu.", createdFilm));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(
                    new ErrorMessage(MessageType.INTERNAL_ERROR, "Film kaydedilirken hata oluştu")
            );
        }
    }

    // --- GET ALL ---
    @GetMapping(path = "/list")
    @Override
    public ResponseEntity<ApiSuccess<List<DtoFilm>>> getAllFilms(
            @RequestParam(required = false) ListingType type
    ) {
        try {
            List<DtoFilm> films = filmService.getAllFilms(type);
            return ResponseEntity.ok(ApiSuccess.of("İçerikler başarıyla listelendi.", films));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(
                    new ErrorMessage(MessageType.INTERNAL_ERROR, "İçerikler listelenirken hata oluştu")
            );
        }
    }

    // --- GET BY ID ---
    @GetMapping(path = "/list/{id}")
    @Override
    public ResponseEntity<ApiSuccess<DtoFilm>> getFilmById(@PathVariable(name = "id") Long id) {
        try {
            DtoFilm film = filmService.getFilmById(id);
            return ResponseEntity.ok(ApiSuccess.of("Film başarıyla bulundu.", film));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(
                    new ErrorMessage(MessageType.NOT_FOUND, "Film bulunamadı")
            );
        }
    }

    // --- DELETE ---
    @DeleteMapping(path = "/delete/{id}")
    @Override
    public ResponseEntity<ApiSuccess<String>> deleteFilmById(@PathVariable(name = "id") Long id) {
        try {
            String result = filmService.deleteFilmById(id);
            return ResponseEntity.ok(ApiSuccess.of("Film başarıyla silindi.", result));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(
                    new ErrorMessage(MessageType.INTERNAL_ERROR, "Film silinirken hata oluştu")
            );
        }
    }

    // --- UPDATE ---
    @PutMapping(path = "/update/{id}")
    @Override
    public ResponseEntity<ApiSuccess<DtoFilm>> updateFilm(
            @RequestBody DtoFilmIU dtoFilm,
            @PathVariable(name = "id") Long id
    ) {
        try {
            DtoFilm updatedFilm = filmService.updateFilm(dtoFilm, id);
            return ResponseEntity.ok(ApiSuccess.of("Film başarıyla güncellendi.", updatedFilm));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(
                    new ErrorMessage(MessageType.INTERNAL_ERROR, "Film güncellenirken hata oluştu")
            );
        }
    }

    // --- SEARCH ---
    @GetMapping("/search")
    public ResponseEntity<ApiSuccess<List<DtoFilm>>> searchFilms(@RequestParam String query) {
        try {
            List<DtoFilm> results = filmService.searchFilms(query);
            return ResponseEntity.ok(ApiSuccess.of("Arama sonuçları", results));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {

            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Arama sırasında hata oluştu"));
        }
    }

}