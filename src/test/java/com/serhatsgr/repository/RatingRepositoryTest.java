package com.serhatsgr.repository;

import com.serhatsgr.entity.Film;
import com.serhatsgr.entity.ListingType;
import com.serhatsgr.entity.Rating;
import com.serhatsgr.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = RatingRepositoryTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS MOVIE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class RatingRepositoryTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan("com.serhatsgr.entity")
    @EnableJpaRepositories("com.serhatsgr.repository")
    static class TestConfig {}

    @Autowired private RatingRepository ratingRepository;
    @Autowired private FilmRepository filmRepository;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName("getAverageRating & getRatingCount -> İstatistikleri doğru hesaplamalı")
    void calculateStatistics_Success() {
        // Given
        Film film = new Film();
        film.setTitle("Test Movie");
        film.setReleaseDate(LocalDate.now());
        film.setListingType(ListingType.VISION);
        film.setPosterUrl("http://poster.url");
        film.setTrailerUrl("http://trailer.url");
        film.setDescription("Test Description");

        filmRepository.save(film);

        User user1 = new User(); user1.setUsername("u1"); user1.setEmail("u1@test.com");
        userRepository.save(user1);

        User user2 = new User(); user2.setUsername("u2"); user2.setEmail("u2@test.com");
        userRepository.save(user2);

        Rating r1 = new Rating(); r1.setFilm(film); r1.setUser(user1); r1.setScore(4);
        Rating r2 = new Rating(); r2.setFilm(film); r2.setUser(user2); r2.setScore(5);
        ratingRepository.save(r1);
        ratingRepository.save(r2);

        // When
        Double average = ratingRepository.getAverageRating(film.getId());
        Integer count = ratingRepository.getRatingCount(film.getId());

        // Then
        assertThat(average).isEqualTo(4.5);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Hiç oy yoksa ortalama NULL dönmeli")
    void calculateStatistics_NoRatings() {
        // Given
        Film film = new Film();
        film.setTitle("Empty Movie");
        film.setReleaseDate(LocalDate.now());
        film.setListingType(ListingType.VISION);
        film.setPosterUrl("http://poster2.url");
        film.setTrailerUrl("http://trailer2.url");
        film.setDescription("Test Desc");

        filmRepository.save(film);

        // When
        Double average = ratingRepository.getAverageRating(film.getId());
        Integer count = ratingRepository.getRatingCount(film.getId());

        // Then
        assertThat(average).isNull();
        assertThat(count).isEqualTo(0);
    }
}