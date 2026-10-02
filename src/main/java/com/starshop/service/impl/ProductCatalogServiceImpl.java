package com.starshop.service.impl;

import com.starshop.dto.OptionDto;
import com.starshop.dto.product.ProductCardDto;
import com.starshop.dto.product.ProductSearchCriteria;
import com.starshop.entity.Category;
import com.starshop.entity.Product;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.mapper.ProductMapper;
import com.starshop.repository.CategoryRepository;
import com.starshop.repository.ProductImageRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.ShopRepository;
import com.starshop.repository.spec.ProductSpecifications;
import com.starshop.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final ShopRepository shopRepository;

    @Override
    public Page<ProductCardDto> bestSellers(int page) {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.visibleToCustomers(),
                ProductSpecifications.soldMoreThan(BEST_SELLER_MIN_SOLD));
        Sort sort = Sort.by(Sort.Direction.DESC, "soldCount").and(Sort.by(Sort.Direction.DESC, "id"));
        return toCards(productRepository.findAll(spec, PageRequest.of(Math.max(page, 0), PAGE_SIZE, sort)));
    }

    @Override
    public Page<ProductCardDto> search(ProductSearchCriteria criteria) {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.visibleToCustomers(),
                ProductSpecifications.nameContains(criteria.getKeyword()),
                ProductSpecifications.categoryIn(categoryWithChildren(criteria.getCategoryId())),
                ProductSpecifications.priceFrom(criteria.getMinPrice()),
                ProductSpecifications.priceTo(criteria.getMaxPrice()),
                ProductSpecifications.shop(criteria.getShopId()),
                ProductSpecifications.ratingAtLeast(criteria.getRating()));
        PageRequest pageable = PageRequest.of(criteria.getPage() - 1, PAGE_SIZE, criteria.getSortOption().getSort());
        return toCards(productRepository.findAll(spec, pageable));
    }

    @Override
    public List<OptionDto> activeShops() {
        return shopRepository.findByStatusOrderByNameAsc(ShopStatus.APPROVED).stream()
                .map(s -> new OptionDto(s.getId(), s.getName()))
                .toList();
    }

    /** Ảnh đại diện của cả trang lấy bằng 1 query rồi ghép vào card. */
    private Page<ProductCardDto> toCards(Page<Product> products) {
        List<Long> ids = products.getContent().stream().map(Product::getId).toList();
        Map<Long, String> images = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : productImageRepository.findImageUrlsByProductIds(ids)) {
                images.putIfAbsent((Long) row[0], (String) row[1]);
            }
        }
        return products.map(p -> ProductMapper.toCard(p, images.get(p.getId())));
    }

    /**
     * Lọc theo danh mục thì lấy cả các danh mục con/cháu (chọn "Hoa tươi" thấy luôn sản phẩm "Hoa hồng").
     * Id không tồn tại -> trả về {-1} để không ra kết quả nào (thay vì bỏ lọc).
     */
    private Set<Long> categoryWithChildren(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Map<Long, List<Long>> children = new HashMap<>();
        boolean exists = false;
        for (Category c : categoryRepository.findAll()) {
            exists |= c.getId().equals(categoryId);
            if (c.getParent() != null) {
                children.computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>()).add(c.getId());
            }
        }
        if (!exists) {
            return Set.of(-1L);
        }
        Set<Long> result = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>(List.of(categoryId));
        while (!queue.isEmpty()) {
            Long id = queue.pop();
            if (result.add(id)) {
                queue.addAll(children.getOrDefault(id, List.of()));
            }
        }
        return result;
    }
}
