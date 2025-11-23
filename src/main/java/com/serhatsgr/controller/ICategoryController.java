package com.serhatsgr.controller;

import com.serhatsgr.dto.DtoCategory;
import com.serhatsgr.dto.DtoCategoryIU;
import com.serhatsgr.dto.ApiSuccess;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ICategoryController {

    ResponseEntity<ApiSuccess<DtoCategory>> createCategory(DtoCategoryIU dtoCategoryIU);

    ResponseEntity<ApiSuccess<List<DtoCategory>>> getAllCategories();

    ResponseEntity<ApiSuccess<DtoCategory>> getCategoryById(Long id);

    ResponseEntity<ApiSuccess<String>> deleteCategory(Long id);

    ResponseEntity<ApiSuccess<DtoCategory>> updateCategory(DtoCategoryIU dtoCategoryIU, Long id);
}
