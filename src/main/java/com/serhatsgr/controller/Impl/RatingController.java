package com.serhatsgr.controller.Impl;

import com.serhatsgr.dto.ApiSuccess;
import com.serhatsgr.dto.RatingRequest;
import com.serhatsgr.dto.UserRatingResponse;
import com.serhatsgr.service.Impl.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/api/movies/{movieId}/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<ApiSuccess<String>> rateMovie(
            @PathVariable Long movieId,
            @Valid @RequestBody RatingRequest request) {
        ratingService.createOrUpdateRating(movieId, request);
        return ResponseEntity.ok(ApiSuccess.of("Oylama başarılı", "Success"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiSuccess<UserRatingResponse>> getMyRating(@PathVariable Long movieId) {
        return ResponseEntity.ok(ApiSuccess.of("Kullanıcı oyu", ratingService.getUserRating(movieId)));
    }

    @DeleteMapping
    public ResponseEntity<ApiSuccess<String>> deleteRating(@PathVariable Long movieId) {
        ratingService.deleteRating(movieId);
        return ResponseEntity.ok(ApiSuccess.of("Oylama silindi", "Deleted"));
    }
}