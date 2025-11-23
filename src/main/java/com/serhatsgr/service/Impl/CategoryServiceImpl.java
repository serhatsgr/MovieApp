package com.serhatsgr.service.Impl;

import com.serhatsgr.dto.DtoCategory;
import com.serhatsgr.dto.DtoCategoryIU;
import com.serhatsgr.entity.Category;
import com.serhatsgr.entity.Film;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.mapper.CategoryMapper;
import com.serhatsgr.repository.CategoryRepository;
import com.serhatsgr.repository.FilmRepository;
import com.serhatsgr.service.ICategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
@Transactional
public class CategoryServiceImpl implements ICategoryService {

    private final CategoryRepository categoryRepository;
    private final FilmRepository filmRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               FilmRepository filmRepository,
                               CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.filmRepository = filmRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public DtoCategory createCategory(DtoCategoryIU dto) {
        log.info("Kategori oluşturma isteği alındı: {}", dto);

        if (dto == null) {
            throw new BaseException(new ErrorMessage(MessageType.BAD_REQUEST, "Kategori bilgisi boş olamaz."));
        }

        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new BaseException(new ErrorMessage(MessageType.DUPLICATE_RESOURCE,
                    "Bu isimde bir kategori zaten mevcut: " + dto.getName()));
        }

        Category category = categoryMapper.toEntity(dto);
        Category savedCategory = categoryRepository.save(category);
        log.info("Kategori başarıyla oluşturuldu. ID: {}", savedCategory.getId());

        return categoryMapper.toDto(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DtoCategory> getAllCategories() {
        log.debug("Tüm kategoriler getiriliyor...");
        List<Category> categories = categoryRepository.findAll();

        if (categories.isEmpty()) {
            throw new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Hiç kategori bulunamadı."));
        }

        return categoryMapper.toDtoList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    public DtoCategory getCategoryById(Long id) {
        log.debug("Kategori ID ile aranıyor: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BaseException(
                        new ErrorMessage(MessageType.NOT_FOUND, "Kategori bulunamadı: " + id)
                ));

        return categoryMapper.toDto(category);
    }

    @Override
    public String deleteCategory(Long id) {
        log.warn("Kategori silme işlemi başlatıldı. ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BaseException(
                        new ErrorMessage(MessageType.NOT_FOUND, "Silinecek kategori bulunamadı: " + id)
                ));

        // Film bağlantılarını kaldır
        for (Film film : new HashSet<>(category.getFilms())) {
            category.removeFilm(film);
            filmRepository.save(film);
        }

        categoryRepository.delete(category);
        log.info("Kategori başarıyla silindi: {}", category.getName());

        return String.format("'%s' kategorisi başarıyla silindi.", category.getName());
    }

    @Override
    public DtoCategory updateCategory(DtoCategoryIU dto, Long id) {
        log.info("Kategori güncelleme isteği alındı. ID: {}, Veri: {}", id, dto);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BaseException(
                        new ErrorMessage(MessageType.NOT_FOUND, "Güncellenecek kategori bulunamadı: " + id)
                ));

        // Aynı isimde başka bir kategori var mı kontrol et
        boolean nameTaken = categoryRepository.existsByNameIgnoreCase(dto.getName())
                && !category.getName().equalsIgnoreCase(dto.getName());
        if (nameTaken) {
            throw new BaseException(new ErrorMessage(MessageType.DUPLICATE_RESOURCE,
                    "Bu isim başka bir kategoriye ait: " + dto.getName()));
        }

        // İsteğe bağlı alan güncellemeleri
        if (dto.getName() != null && !dto.getName().isBlank()) {
            category.setName(dto.getName());
        }
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            category.setDescription(dto.getDescription());
        }

        Category updated = categoryRepository.save(category);
        log.info("Kategori başarıyla güncellendi. ID: {}", id);

        return categoryMapper.toDto(updated);
    }
}
