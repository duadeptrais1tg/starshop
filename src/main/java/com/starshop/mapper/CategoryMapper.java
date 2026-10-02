package com.starshop.mapper;

import com.starshop.dto.category.CategoryDto;
import com.starshop.dto.category.CategoryForm;
import com.starshop.entity.Category;
import com.starshop.util.DateFormats;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryDto toDto(Category category, long productCount) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .imageUrl(category.getImageUrl())
                .active(category.isActive())
                .parentId(category.getParent() == null ? null : category.getParent().getId())
                .parentName(category.getParent() == null ? null : category.getParent().getName())
                .productCount(productCount)
                .createdAt(DateFormats.dateTime(category.getCreatedAt()))
                .build();
    }

    public static CategoryForm toForm(Category category) {
        CategoryForm form = new CategoryForm();
        form.setName(category.getName());
        form.setSlug(category.getSlug());
        form.setParentId(category.getParent() == null ? null : category.getParent().getId());
        form.setActive(category.isActive());
        form.setCurrentImageUrl(category.getImageUrl());
        return form;
    }
}
