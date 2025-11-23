package com.serhatsgr.mapper;

import com.serhatsgr.dto.DtoCategory;
import com.serhatsgr.dto.DtoCategoryIU;
import com.serhatsgr.dto.DtoFilmSummary;
import com.serhatsgr.entity.Category;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategoryMapper {

    //entity'e dönüştürme
    public Category toEntity(DtoCategoryIU dtoCategoryIU) {
        Category category = new Category();
        category.setName(dtoCategoryIU.getName());
        category.setDescription(dtoCategoryIU.getDescription());
        return category;
    }

    //dto'ya dönüştürme
    public DtoCategory toDto(Category category) {
        DtoCategory dtoCategory=new DtoCategory();
        dtoCategory.setName(category.getName());
        dtoCategory.setDescription(category.getDescription());


        // Film başlıklarını ve poster URL'lerini bir listeye dönüştürme
        List<DtoFilmSummary> filmSummary = category.getFilms().stream()
                .map(film -> new DtoFilmSummary(film.getTitle(), film.getPosterUrl()))
                .collect(Collectors.toList());

        dtoCategory.setFilmSummaries(filmSummary);
        return dtoCategory;
    }

    //birden fazla kategori
    public List<DtoCategory> toDtoList(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of(); // boş liste dön
        }

        return categories.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
