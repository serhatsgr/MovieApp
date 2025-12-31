package com.serhatsgr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhatsgr.controller.Impl.FilmControllerImpl;
import com.serhatsgr.dto.DtoFilm;
import com.serhatsgr.dto.DtoFilmIU;
import com.serhatsgr.entity.ListingType;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.handler.GlobalExceptionHandler;
import com.serhatsgr.service.IFilmService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FilmControllerImpl.class)
@ContextConfiguration(classes = FilmControllerTest.TestConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class FilmControllerTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({FilmControllerImpl.class, GlobalExceptionHandler.class})
    static class TestConfig {}

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private IFilmService filmService;


    // ============================================================
    //                --- GET ALL FILMS ---
    // ============================================================
    @Test
    @DisplayName("GET /list -> Tüm filmleri getir")
    void getAllFilms_Success() throws Exception {
        DtoFilm film = new DtoFilm();
        film.setTitle("Matrix");
        given(filmService.getAllFilms(null)).willReturn(List.of(film));

        mockMvc.perform(get("/rest/api/film/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Matrix"));
    }

    @Test
    @DisplayName("GET /list?type=VIZYON -> Type parametresi ile listele")
    void getAllFilms_ByType_Success() throws Exception {
        DtoFilm film = new DtoFilm();
        film.setTitle("Avatar");
        // Parametreye göre ListingType Enum gönderilmeli
        given(filmService.getAllFilms(ListingType.VISION)).willReturn(List.of(film));

        // URL parametresi backend'deki enum ile eşleşmeli (VISION)
        mockMvc.perform(get("/rest/api/film/list").param("type", "VISION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Avatar"));
    }


    // ============================================================
    //                --- GET FILM BY ID ---
    // ============================================================
    @Test
    @DisplayName("GET /list/{id} -> Film detay getir")
    void getFilmById_Success() throws Exception {
        DtoFilm film = new DtoFilm();
        film.setTitle("Inception");
        given(filmService.getFilmById(1L)).willReturn(film);

        mockMvc.perform(get("/rest/api/film/list/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Inception"));
    }

    @Test
    @DisplayName("GET /list/{id} -> Film bulunamadı (error case)")
    void getFilmById_Error() throws Exception {
        given(filmService.getFilmById(99L))
                .willThrow(new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Film yok")));

        mockMvc.perform(get("/rest/api/film/list/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Film yok"));
    }


    // ============================================================
    //                --- CREATE FILM ---
    // ============================================================
    @Test
    @DisplayName("POST /save -> Film kaydet")
    void saveFilm_Success() throws Exception {
        DtoFilmIU req = new DtoFilmIU();
        req.setTitle("Interstellar");
        req.setDescription("Dünyanın sonu gelirken bir grup astronot yaşanabilir gezegen arayışına çıkar...");
        req.setReleaseDate(java.time.LocalDate.parse("2014-11-07"));
        req.setPosterUrl("https://image.tmdb.org/t/p/original/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg");
        req.setTrailerUrl("https://www.youtube.com/watch?v=zSWdZVtXT7E");
        req.setCategoryIds(List.of(1L, 2L, 3L)); // Örnek Kategori ID'leri
        req.setListingType(ListingType.VISION);   // Enum değeri (VISION, ARCHIVE vb.)

        // Service'den dönecek olan Response DTO
        DtoFilm res = new DtoFilm();
        res.setId(101L); // Veritabanından atanmış ID simülasyonu
        res.setTitle("Interstellar");
        res.setDescription(req.getDescription());
        res.setReleaseDate(java.time.LocalDate.parse("2014-11-07"));
        res.setPosterUrl(req.getPosterUrl());

        // Mocklama: Service'in addFilm metodu çağrıldığında yukarıdaki 'res' nesnesini dön
        given(filmService.addFilm(any(DtoFilmIU.class))).willReturn(res);

        // 2. WHEN & THEN (Eylem ve Doğrulama)
        mockMvc.perform(post("/rest/api/film/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))) // Request'i JSON'a çevir
                .andExpect(status().isOk()) // HTTP 200 Bekleniyor
                // Backend'in dönüş formatına göre kontroller (ApiSuccess -> data -> fields)
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.title").value("Interstellar"));
    }

    @Test
    @DisplayName("POST /save -> Film kaydet hata (error case)")
    void saveFilm_Error() throws Exception {
        DtoFilmIU req = new DtoFilmIU();
        req.setTitle("Test Film");
        req.setDescription("Test Açıklama alanı en az birkaç karakter olmalı...");
        req.setReleaseDate(java.time.LocalDate.now()); // LocalDate tipinde
        req.setPosterUrl("https://valid-url.com/poster.jpg");
        req.setTrailerUrl("https://valid-url.com/trailer");
        req.setCategoryIds(List.of(1L));
        req.setListingType(ListingType.VISION);

        // Service katmanının hata fırlattığını simüle et
        given(filmService.addFilm(any(DtoFilmIU.class)))
                .willThrow(new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Film kaydedilemedi")));

        // 2. WHEN & THEN
        mockMvc.perform(post("/rest/api/film/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError()) // 500 Bekleniyor
                // GlobalExceptionHandler tarafından döndürülen JSON formatı
                .andExpect(jsonPath("$.message").value("Film kaydedilemedi"));
    }


    // ============================================================
    //                --- UPDATE FILM ---
    // ============================================================
    @Test
    @DisplayName("PUT /update/{id} -> Film güncelle")
    void updateFilm_Success() throws Exception {
        DtoFilmIU req = new DtoFilmIU();
        req.setTitle("Updated Film");

        DtoFilm res = new DtoFilm();
        res.setTitle("Updated Film");

        given(filmService.updateFilm(any(DtoFilmIU.class), eq(1L))).willReturn(res);

        mockMvc.perform(put("/rest/api/film/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Film"));
    }

    @Test
    @DisplayName("PUT /update/{id} -> Film güncellenemedi (error case)")
    void updateFilm_Error() throws Exception {
        given(filmService.updateFilm(any(DtoFilmIU.class), eq(1L)))
                .willThrow(new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Güncellenemedi")));

        DtoFilmIU req = new DtoFilmIU();
        req.setTitle("X");

        mockMvc.perform(put("/rest/api/film/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Güncellenemedi"));
    }


    // ============================================================
    //                --- DELETE FILM ---
    // ============================================================
    @Test
    @DisplayName("DELETE /delete/{id} -> Film sil")
    void deleteFilm_Success() throws Exception {
        given(filmService.deleteFilmById(1L)).willReturn("Film silindi");

        mockMvc.perform(delete("/rest/api/film/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Film silindi"));
    }

    @Test
    @DisplayName("DELETE /delete/{id} -> Film silinemedi (error case)")
    void deleteFilm_Error() throws Exception {
        given(filmService.deleteFilmById(1L))
                .willThrow(new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Silinemedi")));

        mockMvc.perform(delete("/rest/api/film/delete/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Silinemedi"));
    }


    // ============================================================
    //                --- SEARCH FILM ---
    // ============================================================
    @Test
    @DisplayName("GET /search?query=abc -> Arama sonuçları")
    void searchFilms_Success() throws Exception {
        DtoFilm f = new DtoFilm();
        f.setTitle("Terminator");

        given(filmService.searchFilms("ter")).willReturn(List.of(f));

        mockMvc.perform(get("/rest/api/film/search").param("query", "ter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Terminator"));
    }

    @Test
    @DisplayName("GET /search?query=x -> Arama hatası (error case)")
    void searchFilms_Error() throws Exception {
        given(filmService.searchFilms("x"))
                .willThrow(new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Arama hatası")));

        mockMvc.perform(get("/rest/api/film/search").param("query", "x"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Arama hatası"));
    }

}
