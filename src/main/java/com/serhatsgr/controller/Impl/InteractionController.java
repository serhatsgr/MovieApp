package com.serhatsgr.controller.Impl;

import com.serhatsgr.dto.ApiSuccess;
import com.serhatsgr.dto.DtoFilm;
import com.serhatsgr.service.Impl.FavoriteService;
import com.serhatsgr.service.Impl.WatchedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/rest/api/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final FavoriteService favoriteService;
    private final WatchedService watchedService;

    // --- FAVORITES ---
    @GetMapping("/favorites")
    public ResponseEntity<ApiSuccess<List<DtoFilm>>> getFavorites() {
        return ResponseEntity.ok(ApiSuccess.of("Favoriler", favoriteService.getMyFavorites()));
    }

    @PostMapping("/favorites/{filmId}")
    public ResponseEntity<ApiSuccess<String>> addFavorite(@PathVariable Long filmId) {
        favoriteService.addFavorite(filmId);
        return ResponseEntity.ok(ApiSuccess.of("Favorilere eklendi", "Added"));
    }

    @DeleteMapping("/favorites/{filmId}")
    public ResponseEntity<ApiSuccess<String>> removeFavorite(@PathVariable Long filmId) {
        favoriteService.removeFavorite(filmId);
        return ResponseEntity.ok(ApiSuccess.of("Favorilerden çıkarıldı", "Removed"));
    }

    // --- WATCHED ---
    @GetMapping("/watched")
    public ResponseEntity<ApiSuccess<List<DtoFilm>>> getWatched() {
        return ResponseEntity.ok(ApiSuccess.of("İzlenenler", watchedService.getMyWatchedList()));
    }

    @PostMapping("/watched/{filmId}")
    public ResponseEntity<ApiSuccess<String>> markWatched(@PathVariable Long filmId) {
        watchedService.markAsWatched(filmId);
        return ResponseEntity.ok(ApiSuccess.of("İzledim olarak işaretlendi", "Marked"));
    }

    @DeleteMapping("/watched/{filmId}")
    public ResponseEntity<ApiSuccess<String>> unmarkWatched(@PathVariable Long filmId) {
        watchedService.unmarkWatched(filmId);
        return ResponseEntity.ok(ApiSuccess.of("İzledim işareti kaldırıldı", "Unmarked"));
    }
}