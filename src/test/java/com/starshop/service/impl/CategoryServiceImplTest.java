package com.starshop.service.impl;

import com.starshop.dto.OptionDto;
import com.starshop.dto.UploadResult;
import com.starshop.dto.category.CategoryForm;
import com.starshop.entity.Category;
import com.starshop.entity.enums.MediaType;
import com.starshop.exception.BusinessException;
import com.starshop.exception.InvalidFileException;
import com.starshop.repository.CategoryRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryServiceImplTest {

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final FileStorageService storage = mock(FileStorageService.class);
    private final CategoryServiceImpl service = new CategoryServiceImpl(categoryRepository, productRepository, storage);

    private Category root;
    private Category child;
    private Category grandChild;

    @BeforeEach
    void setUp() {
        root = category(1L, "Hoa tươi", "hoa-tuoi", null);
        child = category(2L, "Hoa hồng", "hoa-hong", root);
        grandChild = category(3L, "Hồng đỏ", "hong-do", child);
        for (Category c : List.of(root, child, grandChild)) {
            when(categoryRepository.findById(c.getId())).thenReturn(Optional.of(c));
        }
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(child, grandChild, root));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });
        when(storage.uploadImage(any(), eq("categories")))
                .thenReturn(new UploadResult("https://img/new.jpg", "starshop/categories/new", MediaType.IMAGE));
    }

    // ----------------------------------------------------------------- tạo mới

    @Test
    void create_generatesSlugFromVietnameseName() {
        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);

        service.create(form("  Hoa Khai Trương ", "", null), null);

        verify(categoryRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Hoa Khai Trương");
        assertThat(saved.getValue().getSlug()).isEqualTo("hoa-khai-truong");
    }

    @Test
    void create_autoSlugConflict_addsNumberSuffix() {
        when(categoryRepository.existsBySlug("hoa-cuoi")).thenReturn(true);
        when(categoryRepository.existsBySlug("hoa-cuoi-2")).thenReturn(true);
        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);

        service.create(form("Hoa cưới", null, null), null);

        verify(categoryRepository).save(saved.capture());
        assertThat(saved.getValue().getSlug()).isEqualTo("hoa-cuoi-3");
    }

    @Test
    void create_duplicateNameOrTypedSlug_isRejected_withoutUploadingImage() {
        when(categoryRepository.existsByNameIgnoreCase("hoa tươi")).thenReturn(true);
        assertThatThrownBy(() -> service.create(form("hoa tươi", null, null), image()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("đã tồn tại");

        when(categoryRepository.existsBySlug("hoa-tuoi")).thenReturn(true);
        assertThatThrownBy(() -> service.create(form("Tên khác", "hoa-tuoi", null), image()))
                .hasMessageContaining("Slug");

        verify(storage, never()).uploadImage(any(), anyString());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_withImage_storesUrlAndPublicId() {
        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);

        service.create(form("Hoa lan", null, 1L), image());

        verify(categoryRepository).save(saved.capture());
        assertThat(saved.getValue().getImageUrl()).isEqualTo("https://img/new.jpg");
        assertThat(saved.getValue().getImagePublicId()).isEqualTo("starshop/categories/new");
        assertThat(saved.getValue().getParent()).isSameAs(root);
    }

    @Test
    void create_invalidImage_propagatesError() {
        when(storage.uploadImage(any(), anyString())).thenThrow(new InvalidFileException("Chỉ chấp nhận file jpg, png, webp."));
        assertThatThrownBy(() -> service.create(form("Hoa lan", null, null), image()))
                .isInstanceOf(InvalidFileException.class);
        verify(categoryRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- cha / con

    @Test
    void update_cannotPickItselfOrDescendantAsParent() {
        assertThatThrownBy(() -> service.update(1L, form("Hoa tươi", "hoa-tuoi", 1L), null))
                .hasMessageContaining("danh mục cha");
        assertThatThrownBy(() -> service.update(1L, form("Hoa tươi", "hoa-tuoi", 3L), null))
                .hasMessageContaining("danh mục cha");
        assertThat(root.getParent()).isNull();
    }

    @Test
    void parentOptions_excludeSelfAndDescendants() {
        List<Long> ids = service.parentOptions(2L).stream().map(OptionDto::getId).toList();
        assertThat(ids).containsExactly(1L);

        assertThat(service.parentOptions(null)).hasSize(3);
    }

    // -------------------------------------------------------------------- ảnh

    @Test
    void update_newImage_replacesAndDeletesOldOne() {
        child.setImagePublicId("starshop/categories/old");

        service.update(2L, form("Hoa hồng", "hoa-hong", 1L), image());

        assertThat(child.getImagePublicId()).isEqualTo("starshop/categories/new");
        verify(storage).delete("starshop/categories/old", MediaType.IMAGE);
    }

    @Test
    void update_removeImage_clearsIt() {
        child.setImageUrl("https://img/old.jpg");
        child.setImagePublicId("starshop/categories/old");
        CategoryForm form = form("Hoa hồng", "hoa-hong", 1L);
        form.setRemoveImage(true);

        service.update(2L, form, null);

        assertThat(child.getImageUrl()).isNull();
        verify(storage).delete("starshop/categories/old", MediaType.IMAGE);
    }

    // -------------------------------------------------------------- ẩn / xóa

    @Test
    void toggle_switchesVisibility() {
        service.toggleActive(3L);
        assertThat(grandChild.isActive()).isFalse();
        service.toggleActive(3L);
        assertThat(grandChild.isActive()).isTrue();
    }

    @Test
    void delete_blockedWhenHasProducts_orChildren() {
        when(productRepository.countByCategoryId(3L)).thenReturn(4L);
        assertThatThrownBy(() -> service.delete(3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("4 sản phẩm").hasMessageContaining("ẩn");

        when(categoryRepository.existsByParentId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L)).hasMessageContaining("danh mục con");

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void delete_emptyLeaf_removesItAndItsImage() {
        grandChild.setImagePublicId("starshop/categories/x");

        service.delete(3L);

        verify(categoryRepository).delete(grandChild);
        verify(storage).delete("starshop/categories/x", MediaType.IMAGE);
    }

    // ---------------------------------------------------------------- helpers

    private static Category category(Long id, String name, String slug, Category parent) {
        Category c = Category.builder().name(name).slug(slug).parent(parent).build();
        c.setId(id);
        return c;
    }

    private static CategoryForm form(String name, String slug, Long parentId) {
        CategoryForm form = new CategoryForm();
        form.setName(name);
        form.setSlug(slug);
        form.setParentId(parentId);
        form.setActive(true);
        return form;
    }

    private static MockMultipartFile image() {
        return new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
    }
}
