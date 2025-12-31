package com.serhatsgr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhatsgr.controller.Impl.CategoryController;
import com.serhatsgr.dto.DtoCategory;
import com.serhatsgr.dto.DtoCategoryIU;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.handler.GlobalExceptionHandler;
import com.serhatsgr.service.ICategoryService;
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

@WebMvcTest(CategoryController.class)
@ContextConfiguration(classes = CategoryControllerTest.TestConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    // --- CONFIGURATION ---
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({CategoryController.class, GlobalExceptionHandler.class})
    static class TestConfig {}
    // ---------------------

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ICategoryService categoryService;

    @Test
    @DisplayName("GET /list -> Tüm kategorileri getir")
    void getAllCategories_Success() throws Exception {
        // Given
        DtoCategory cat1 = new DtoCategory(1L, "Action", "Desc", null);
        given(categoryService.getAllCategories()).willReturn(List.of(cat1));

        // When & Then
        mockMvc.perform(get("/rest/api/category/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Action"));
    }

    @Test
    @DisplayName("GET /list/{id} -> ID ile kategori getir")
    void getCategoryById_Success() throws Exception {
        // Given
        DtoCategory cat = new DtoCategory(1L, "Horror", "Scary movies", null);
        given(categoryService.getCategoryById(1L)).willReturn(cat);

        // When & Then
        mockMvc.perform(get("/rest/api/category/list/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Horror"));
    }

    @Test
    @DisplayName("GET /list/{id} -> Kategori bulunamadığında hata fırlatır")
    void getCategoryById_NotFound() throws Exception {
        // Given
        given(categoryService.getCategoryById(99L))
                .willThrow(new BaseException(
                        new ErrorMessage(MessageType.NOT_FOUND, "Kategori bulunamadı")
                ));

        // When & Then
        mockMvc.perform(get("/rest/api/category/list/99"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value("Kategori bulunamadı"));
    }

    @Test
    @DisplayName("POST /create -> Kategori oluştur")
    void createCategory_Success() throws Exception {
        // Given
        DtoCategoryIU req = new DtoCategoryIU("Drama", "Drama Movies");
        DtoCategory res = new DtoCategory(1L, "Drama", "Drama Movies", null);

        given(categoryService.createCategory(any(DtoCategoryIU.class))).willReturn(res);

        // When & Then
        mockMvc.perform(post("/rest/api/category/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Drama"));
    }

    @Test
    @DisplayName("POST /create -> Kategori oluşturma hata fırlatır")
    void createCategory_Error() throws Exception {
        // Given
        DtoCategoryIU req = new DtoCategoryIU("Drama", "Drama Movies");

        given(categoryService.createCategory(any(DtoCategoryIU.class)))
                .willThrow(new BaseException(
                        new ErrorMessage(MessageType.INTERNAL_ERROR, "Kategori oluşturulamadı")
                ));

        // When & Then
        mockMvc.perform(post("/rest/api/category/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Kategori oluşturulamadı"));
    }


    @Test
    @DisplayName("PUT /update/{id} -> Kategori güncelle")
    void updateCategory_Success() throws Exception {
        // Given
        DtoCategoryIU req = new DtoCategoryIU("Updated Name", "Updated Desc");
        DtoCategory res = new DtoCategory(1L, "Updated Name", "Updated Desc", null);

        // Parametre sırasına dikkat: (DtoCategoryIU, Long) veya (Long, DtoCategoryIU) backend'e göre
        given(categoryService.updateCategory(any(DtoCategoryIU.class), eq(1L))).willReturn(res);

        // When & Then
        mockMvc.perform(put("/rest/api/category/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Name"));
    }

    @Test
    @DisplayName("PUT /update/{id} -> Güncelleme hata fırlatır")
    void updateCategory_Error() throws Exception {
        // Given
        DtoCategoryIU req = new DtoCategoryIU("Name", "Desc");

        given(categoryService.updateCategory(any(DtoCategoryIU.class), eq(1L)))
                .willThrow(new BaseException(
                        new ErrorMessage(MessageType.INTERNAL_ERROR, "Kategori güncellenemedi")
                ));

        // When & Then
        mockMvc.perform(put("/rest/api/category/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Kategori güncellenemedi"));
    }


    @Test
    @DisplayName("DELETE /delete/{id} -> Kategori sil")
    void deleteCategory_Success() throws Exception {
        // Given
        given(categoryService.deleteCategory(1L)).willReturn("Kategori başarıyla silindi.");

        // When & Then
        mockMvc.perform(delete("/rest/api/category/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Kategori başarıyla silindi."));
    }

    @Test
    @DisplayName("DELETE /delete/{id} -> Silme hata fırlatır")
    void deleteCategory_Error() throws Exception {

        given(categoryService.deleteCategory(1L))
                .willThrow(new BaseException(
                        new ErrorMessage(MessageType.INTERNAL_ERROR, "Kategori silinemedi")
                ));

        mockMvc.perform(delete("/rest/api/category/delete/1"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Kategori silinemedi"));
    }




}