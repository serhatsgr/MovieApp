package com.serhatsgr.service;

import com.serhatsgr.dto.DtoCategory;
import com.serhatsgr.dto.DtoCategoryIU;

import java.util.List;

public interface ICategoryService {

    public DtoCategory createCategory(DtoCategoryIU dtoCategoryIU);

    public List<DtoCategory> getAllCategories();

    public DtoCategory getCategoryById(Long id);

    public String deleteCategory(Long id);

    public DtoCategory updateCategory(DtoCategoryIU dtoCategoryIU, Long id);
}
