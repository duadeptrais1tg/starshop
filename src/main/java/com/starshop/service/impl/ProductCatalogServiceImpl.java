package com.starshop.service.impl;

import com.starshop.dto.OptionDto;
import com.starshop.dto.product.CategoryPageInfo;
import com.starshop.dto.product.HomeBlock;
import com.starshop.dto.product.ProductCardDto;
import com.starshop.dto.product.ProductDetailDto;
import com.starshop.dto.product.ProductSearchCriteria;
import com.starshop.dto.product.ProductSort;
import com.starshop.dto.product.PromotionInfo;
import com.starshop.dto.promotion.AutoPricing;
import com.starshop.entity.Category;
import com.starshop.entity.Coupon;
import com.starshop.entity.Product;
import com.starshop.entity.ProductImage;
import com.starshop.entity.Promotion;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.NotFoundException;
import com.starshop.mapper.ProductMapper;
import com.starshop.repository.CategoryRepository;
import com.starshop.repository.CouponRepository;
import com.starshop.repository.ProductImageRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.PromotionRepository;
import com.starshop.repository.ShopRepository;
import com.starshop.repository.spec.ProductSpecifications;
import com.starshop.service.ProductCatalogService;
import com.starshop.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
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
    private final PromotionRepository promotionRepository;
    private final CouponRepository couponRepository;
    private final Clock clock;
    private final PromotionService promotionService;

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

    @Override
    public Page<ProductCardDto> homeBlock(HomeBlock block, int page) {
        int safePage = Math.max(page, 0);
        PageRequest pageable = PageRequest.of(safePage, HomeBlock.PAGE_SIZE, block.getSort());
        if (safePage >= HomeBlock.MAX_PAGES) {
            return Page.empty(pageable);
        }
        Specification<Product> extra = switch (block) {
            case TOP_RATED -> ProductSpecifications.hasReviews();
            case MOST_FAVORITED -> ProductSpecifications.hasFavorites();
            default -> null;
        };
        Specification<Product> spec = Specification.allOf(ProductSpecifications.visibleToCustomers(), extra);
        return toCards(productRepository.findAll(spec, pageable));
    }

    @Override
    public CategoryPageInfo categoryInfo(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .filter(Category::isActive)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục " + slug));
        CategoryPageInfo.CategoryLink parent = category.getParent() == null ? null
                : new CategoryPageInfo.CategoryLink(category.getParent().getName(), category.getParent().getSlug());
        List<CategoryPageInfo.CategoryLink> children = categoryRepository.findByParentIdAndActiveTrueOrderByNameAsc(category.getId())
                .stream()
                .map(c -> new CategoryPageInfo.CategoryLink(c.getName(), c.getSlug()))
                .toList();
        return new CategoryPageInfo(category.getId(), category.getName(), category.getSlug(),
                category.getImageUrl(), parent, children);
    }

    @Override
    public Page<ProductCardDto> byCategory(Long categoryId, ProductSort sort, int page) {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.visibleToCustomers(),
                ProductSpecifications.categoryIn(categoryWithChildren(categoryId)));
        return toCards(productRepository.findAll(spec, PageRequest.of(Math.max(page, 0), PAGE_SIZE, sort.getSort())));
    }

    @Override
    public ProductDetailDto getDetail(String slug) {
        Product product = productRepository.findBySlug(slug)
                .filter(ProductCatalogServiceImpl::isVisibleToCustomers)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm " + slug));
        List<String> images = productImageRepository.findByProductIdOrderByThumbnailDescSortOrderAsc(product.getId())
                .stream().map(ProductImage::getUrl).toList();
        BigDecimal salePrice = promotionService.autoPricing()
                .salePrice(product.getShop().getId(), product.getCategory().getId(), product.getPrice());
        return ProductMapper.toDetail(product, images, activePromotions(product), salePrice);
    }

    @Override
    public List<ProductCardDto> related(ProductDetailDto product, int limit) {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.visibleToCustomers(),
                ProductSpecifications.categoryIn(Set.of(product.getCategoryId())),
                ProductSpecifications.idNot(product.getId()));
        Sort sort = Sort.by(Sort.Direction.DESC, "soldCount").and(Sort.by(Sort.Direction.DESC, "id"));
        return toCards(productRepository.findAll(spec, PageRequest.of(0, limit, sort))).getContent();
    }

    /** Cùng điều kiện với ProductSpecifications.visibleToCustomers, kiểm tra trên entity đã nạp. */
    private static boolean isVisibleToCustomers(Product p) {
        return p.isActive() && p.getShop().getStatus() == ShopStatus.APPROVED && p.getCategory().isActive();
    }

    /** Khuyến mãi đang chạy cho sản phẩm (toàn sàn / danh mục / shop) kèm mã coupon nếu có. */
    private List<PromotionInfo> activePromotions(Product product) {
        List<Promotion> promotions = promotionRepository.findActiveForProduct(
                LocalDateTime.now(clock), product.getShop().getId(), product.getCategory().getId());
        if (promotions.isEmpty()) {
            return List.of();
        }
        Map<Long, List<String>> codes = new HashMap<>();
        for (Coupon coupon : couponRepository.findByPromotionIdInAndActiveTrue(
                promotions.stream().map(Promotion::getId).toList())) {
            codes.computeIfAbsent(coupon.getPromotion().getId(), k -> new ArrayList<>()).add(coupon.getCode());
        }
        return promotions.stream()
                .map(p -> ProductMapper.toPromotionInfo(p, codes.getOrDefault(p.getId(), List.of())))
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
        // Giá khuyến mãi tự áp dụng: lấy danh sách khuyến mãi đang chạy 1 lần cho cả trang
        AutoPricing pricing = promotionService.autoPricing();
        return products.map(p -> ProductMapper.toCard(p, images.get(p.getId()),
                pricing.salePrice(p.getShop().getId(), p.getCategory().getId(), p.getPrice())));
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
