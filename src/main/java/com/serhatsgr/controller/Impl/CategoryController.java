package com.serhatsgr.controller.Impl;

import com.serhatsgr.controller.ICategoryController;
import com.serhatsgr.dto.DtoCategory;
import com.serhatsgr.dto.DtoCategoryIU;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.dto.ApiSuccess;
import com.serhatsgr.service.ICategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest/api/category")
public class CategoryController implements ICategoryController {

    private final ICategoryService categoryService;

    public CategoryController(ICategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // --- CREATE ---
    @PostMapping("/create")
    @Override
    public ResponseEntity<ApiSuccess<DtoCategory>> createCategory(@RequestBody DtoCategoryIU dtoCategoryIU) {
        try {
            DtoCategory createdCategory = categoryService.createCategory(dtoCategoryIU);
            return ResponseEntity.ok(ApiSuccess.of("Kategori başarıyla oluşturuldu.", createdCategory));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(
                    new ErrorMessage(MessageType.INTERNAL_ERROR, "Kategori oluşturulurken hata oluştu")
            );
        }
    }

    // --- GET ALL ---
    @GetMapping("/list")
    @Override
    public ResponseEntity<ApiSuccess<List<DtoCategory>>> getAllCategories() {
        try {
            List<DtoCategory> categories = categoryService.getAllCategories();
            return ResponseEntity.ok(ApiSuccess.of("Kategoriler başarıyla listelendi.", categories));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(
                    new ErrorMessage(MessageType.INTERNAL_ERROR, "Kategoriler listelenirken hata oluştu")
            );
        }
    }

    // --- GET BY ID ---
    @GetMapping("/list/{id}")
    @Override
    public ResponseEntity<ApiSuccess<DtoCategory>> getCategoryById(@PathVariable Long id) {
        try {
            DtoCategory category = categoryService.getCategoryById(id);
            return ResponseEntity.ok(ApiSuccess.of("Kategori başarıyla bulundu.", category));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(
                    new ErrorMessage(MessageType.NOT_FOUND, "Kategori bulunamadı")
            );
        }
    }

    // --- DELETE ---
    @DeleteMapping("/delete/{id}")
    @Override
    public ResponseEntity<ApiSuccess<String>> deleteCategory(@PathVariable Long id) {
        try {
            String result = categoryService.deleteCategory(id);
            return ResponseEntity.ok(ApiSuccess.of("Kategori başarıyla silindi.", result));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(
                    new ErrorMessage(MessageType.INTERNAL_ERROR, "Kategori silinirken hata oluştu")
            );
        }
    }

    // --- UPDATE ---
    @PutMapping("/update/{id}")
    @Override
    public ResponseEntity<ApiSuccess<DtoCategory>> updateCategory(
            @RequestBody DtoCategoryIU dtoCategoryIU,
            @PathVariable Long id
    ) {
        try {
            DtoCategory updatedCategory = categoryService.updateCategory(dtoCategoryIU, id);
            return ResponseEntity.ok(ApiSuccess.of("Kategori başarıyla güncellendi.", updatedCategory));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(
                    new ErrorMessage(MessageType.INTERNAL_ERROR, "Kategori güncellenirken hata oluştu")
            );
        }
    }
}
