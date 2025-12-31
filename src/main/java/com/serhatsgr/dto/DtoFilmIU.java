package com.serhatsgr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.serhatsgr.entity.ListingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoFilmIU {

    @NotBlank(message = "{film.title.notBlank}")
    @Size(min = 2, max = 100, message = "{film.title.size}")
    private String title;

    @NotBlank(message = "{film.description.notBlank}")
    @Size(min = 10, max = 1000, message = "{film.description.size}")
    private String description;

    @NotNull(message = "{film.releaseDate.notNull}")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;

    @NotBlank(message = "{film.posterUrl.notBlank}")
    @URL(message = "{film.posterUrl.url}")
    private String posterUrl;

    @NotBlank(message = "{film.trailerUrl.notBlank}")
    @URL(message = "{film.trailerUrl.url}")
    private String trailerUrl;

    @NotEmpty(message = "{film.categoryIds.notEmpty}")
    @Size(min = 1, message = "{film.categoryIds.size}")
    private List<Long> categoryIds;

    @NotNull(message = "İçerik tipi boş olamaz (VISION, ARCHIVE, SERIES)")
    private ListingType listingType;
}

