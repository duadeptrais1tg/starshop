package com.starshop.service.impl;

import com.starshop.dto.OptionDto;
import com.starshop.dto.UploadResult;
import com.starshop.dto.category.CategoryDto;
import com.starshop.dto.category.CategoryForm;
import com.starshop.entity.Category;
import com.starshop.entity.enums.MediaType;
import com.starshop.exception.BusinessException;
import com.starshop.exception.FileStorageException;
import com.starshop.exception.NotFoundException;
import com.starshop.mapper.CategoryMapper;
import com.starshop.repository.CategoryRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.spec.CategorySpecifications;
import com.starshop.service.CategoryService;
import com.starshop.service.FileStorageService;
import com.starshop.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String IMAGE_FOLDER = "categories";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryDto> search(String keyword, Boolean active, int page) {
        Page<Category> result = categoryRepository.findAll(
                Specification.allOf(CategorySpecifications.keyword(keyword), CategorySpecifications.active(active)),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("name")));

        // Đếm sản phẩm của cả trang bằng 1 query GROUP BY
        Map<Long, Long> counts = new HashMap<>();
        List<Long> ids = result.getContent().stream().map(Category::getId).toList();
        if (!ids.isEmpty()) {
            for (Object[] row : productRepository.countByCategoryIds(ids)) {
                counts.put((Long) row[0], (Long) row[1]);
            }
        }
        return result.map(c -> CategoryMapper.toDto(c, counts.getOrDefault(c.getId(), 0L)));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryForm getForm(Long id) {
        return CategoryMapper.toForm(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionDto> parentOptions(Long editingId) {
        List<Category> all = categoryRepository.findAllByOrderByNameAsc();
        Set<Long> excluded = editingId == null ? Set.of() : selfAndDescendants(editingId, all);
        return all.stream()
                .filter(c -> !excluded.contains(c.getId()))
                .map(c -> new OptionDto(c.getId(), c.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionDto> activeOptions() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(c -> new OptionDto(c.getId(), c.getName()))
                .toList();
    }

    @Override
    @Transactional
    public Long create(CategoryForm form, MultipartFile image) {
        String name = form.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Tên danh mục \"" + name + "\" đã tồn tại.");
        }
        Category category = new Category();
        category.setName(name);
        category.setSlug(resolveSlug(form.getSlug(), name, null));
        category.setParent(resolveParent(form.getParentId(), null));
        category.setActive(form.isActive());

        // Kiểm tra dữ liệu xong mới upload ảnh, tránh upload rồi mới báo lỗi
        if (hasFile(image)) {
            UploadResult uploaded = upload(image);
            category.setImageUrl(uploaded.url());
            category.setImagePublicId(uploaded.publicId());
        }
        return categoryRepository.save(category).getId();
    }

    @Override
    @Transactional
    public void update(Long id, CategoryForm form, MultipartFile image) {
        Category category = find(id);
        String name = form.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException("Tên danh mục \"" + name + "\" đã tồn tại.");
        }
        category.setName(name);
        category.setSlug(resolveSlug(form.getSlug(), name, id));
        category.setParent(resolveParent(form.getParentId(), id));
        category.setActive(form.isActive());

        String oldPublicId = category.getImagePublicId();
        if (hasFile(image)) {
            UploadResult uploaded = upload(image);
            category.setImageUrl(uploaded.url());
            category.setImagePublicId(uploaded.publicId());
            deleteImageQuietly(oldPublicId);
        } else if (form.isRemoveImage()) {
            category.setImageUrl(null);
            category.setImagePublicId(null);
            deleteImageQuietly(oldPublicId);
        }
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        Category category = find(id);
        category.setActive(!category.isActive());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = find(id);
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new BusinessException("Danh mục \"" + category.getName() + "\" đang có " + productCount
                    + " sản phẩm nên không thể xóa. Bạn có thể ẩn danh mục thay vì xóa.");
        }
        if (categoryRepository.existsByParentId(id)) {
            throw new BusinessException("Danh mục \"" + category.getName()
                    + "\" đang có danh mục con. Hãy chuyển hoặc xóa danh mục con trước.");
        }
        String publicId = category.getImagePublicId();
        categoryRepository.delete(category);
        deleteImageQuietly(publicId);
    }

    // ------------------------------------------------------------------ helpers

    private Category find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục #" + id));
    }

    /**
     * Slug người dùng nhập thì phải không trùng; để trống thì sinh từ tên và tự thêm -2, -3... nếu trùng.
     */
    private String resolveSlug(String requested, String name, Long currentId) {
        if (StringUtils.hasText(requested)) {
            String slug = requested.trim();
            if (slugTaken(slug, currentId)) {
                throw new BusinessException("Slug \"" + slug + "\" đã được dùng cho danh mục khác.");
            }
            return slug;
        }
        String base = SlugUtil.toSlug(name);
        if (base.isEmpty()) {
            throw new BusinessException("Không tạo được slug từ tên, vui lòng nhập slug.");
        }
        String slug = base;
        for (int i = 2; slugTaken(slug, currentId); i++) {
            slug = base + "-" + i;
        }
        return slug;
    }

    private boolean slugTaken(String slug, Long currentId) {
        return currentId == null ? categoryRepository.existsBySlug(slug) : categoryRepository.existsBySlugAndIdNot(slug, currentId);
    }

    /** Danh mục cha không được là chính nó hoặc con/cháu của nó (sẽ tạo vòng lặp). */
    private Category resolveParent(Long parentId, Long currentId) {
        if (parentId == null) {
            return null;
        }
        Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException("Danh mục cha không tồn tại."));
        for (Category c = parent; c != null; c = c.getParent()) {
            if (Objects.equals(c.getId(), currentId)) {
                throw new BusinessException("Không thể chọn chính danh mục này hoặc danh mục con của nó làm danh mục cha.");
            }
        }
        return parent;
    }

    private static Set<Long> selfAndDescendants(Long rootId, List<Category> all) {
        Map<Long, List<Long>> children = new HashMap<>();
        for (Category c : all) {
            if (c.getParent() != null) {
                children.computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>()).add(c.getId());
            }
        }
        Set<Long> result = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>(List.of(rootId));
        while (!queue.isEmpty()) {
            Long id = queue.pop();
            if (result.add(id)) {
                queue.addAll(children.getOrDefault(id, List.of()));
            }
        }
        return result;
    }

    private static boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private UploadResult upload(MultipartFile image) {
        return fileStorageService.uploadImage(image, IMAGE_FOLDER);
    }

    /**
     * Xóa ảnh cũ trên Cloudinary SAU KHI transaction commit (lưu DB lỗi thì ảnh cũ vẫn còn dùng được).
     * Lỗi khi xóa chỉ ghi log, không làm hỏng thao tác chính.
     */
    private void deleteImageQuietly(String publicId) {
        if (!StringUtils.hasText(publicId)) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteNow(publicId);
                }
            });
        } else {
            deleteNow(publicId);
        }
    }

    private void deleteNow(String publicId) {
        try {
            fileStorageService.delete(publicId, MediaType.IMAGE);
        } catch (FileStorageException e) {
            log.warn("Không xóa được ảnh danh mục {} trên Cloudinary: {}", publicId, e.getMessage());
        }
    }
}
