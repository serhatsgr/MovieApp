package com.serhatsgr.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoFilm {

    private Long id;

    private String title;

    private String description;

    private LocalDate releaseDate;

    private String posterUrl;

    private String trailerUrl;

    private List<String> categorys;

    private Double averageRating;

    private Integer ratingCount;

}
