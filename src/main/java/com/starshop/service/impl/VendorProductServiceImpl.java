package com.starshop.service.impl;

import com.starshop.dto.UploadResult;
import com.starshop.dto.product.VendorProductForm;
import com.starshop.dto.product.VendorProductImage;
import com.starshop.dto.product.VendorProductRow;
import com.starshop.dto.product.VendorProductStatus;
import com.starshop.entity.Category;
import com.starshop.entity.Product;
import com.starshop.entity.ProductImage;
import com.starshop.entity.Shop;
import com.starshop.entity.enums.MediaType;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.FileStorageException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.CartItemRepository;
import com.starshop.repository.CategoryRepository;
import com.starshop.repository.FavoriteRepository;
import com.starshop.repository.OrderItemRepository;
import com.starshop.repository.ProductImageRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.ShopRepository;
import com.starshop.repository.ViewedProductRepository;
import com.starshop.repository.spec.ProductSpecifications;
import com.starshop.service.FileStorageService;
import com.starshop.service.VendorProductService;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorProductServiceImpl implements VendorProductService {

    private static final String IMAGE_FOLDER = "products";

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final FavoriteRepository favoriteRepository;
    private final ViewedProductRepository viewedProductRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public Page<VendorProductRow> search(Long ownerId, String keyword, Long categoryId, VendorProductStatus status, int page) {
        Shop shop = requireShop(ownerId);
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.shop(shop.getId()),
                ProductSpecifications.nameContains(keyword),
                ProductSpecifications.category(categoryId),
                status == VendorProductStatus.ACTIVE ? ProductSpecifications.active(true) : null,
                status == VendorProductStatus.INACTIVE ? ProductSpecifications.active(false) : null,
                status == VendorProductStatus.OUT_OF_STOCK ? ProductSpecifications.outOfStock() : null);
        Page<Product> products = productRepository.findAll(spec,
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id")));

        Map<Long, String> images = new HashMap<>();
        if (products.hasContent()) {
            for (Object[] row : imageRepository.findImageUrlsByProductIds(products.map(Product::getId).getContent())) {
                images.putIfAbsent((Long) row[0], (String) row[1]);
            }
        }
        return products.map(p -> VendorProductRow.builder()
                .id(p.getId())
                .name(p.getName())
                .slug(p.getSlug())
                .imageUrl(images.get(p.getId()))
                .categoryName(p.getCategory().getName())
                .price(p.getPrice())
                .originalPrice(p.getOriginalPrice())
                .stock(p.getStock())
                .soldCount(p.getSoldCount())
                .ratingAvg(p.getRatingAvg())
                .reviewCount(p.getReviewCount())
                .active(p.isActive())
                .categoryHidden(!p.getCategory().isActive())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll(Long ownerId) {
        return productRepository.count(ProductSpecifications.shop(requireShop(ownerId).getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public VendorProductForm getForm(Long ownerId, Long productId) {
        Product p = requireOwnProduct(ownerId, productId);
        VendorProductForm form = new VendorProductForm();
        form.setName(p.getName());
        form.setCategoryId(p.getCategory().getId());
        form.setPrice(p.getPrice());
        form.setOriginalPrice(p.getOriginalPrice());
        form.setStock(p.getStock());
        form.setDescription(p.getDescription());
        form.setActive(p.isActive());
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorProductImage> images(Long ownerId, Long productId) {
        requireOwnProduct(ownerId, productId);
        return imageRepository.findByProductIdOrderByThumbnailDescSortOrderAsc(productId).stream()
                .map(i -> new VendorProductImage(i.getId(), i.getUrl(), i.isThumbnail()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String slugOf(Long ownerId, Long productId) {
        return requireOwnProduct(ownerId, productId).getSlug();
    }

    @Override
    @Transactional
    public Long create(Long ownerId, VendorProductForm form, List<MultipartFile> images) {
        Shop shop = requireShop(ownerId);
        List<MultipartFile> files = nonEmpty(images);
        if (files.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất 1 ảnh sản phẩm.");
        }
        if (files.size() > MAX_IMAGES) {
            throw new BusinessException("Mỗi sản phẩm tối đa " + MAX_IMAGES + " ảnh.");
        }
        Product product = Product.builder().shop(shop).build();
        apply(product, form);
        product.setSlug(uniqueSlug(product.getName()));

        addImages(product, files, 0);
        product.getImages().get(0).setThumbnail(true);
        return productRepository.save(product).getId();
    }

    @Override
    @Transactional
    public void update(Long ownerId, Long productId, VendorProductForm form, List<MultipartFile> newImages) {
        Product product = requireOwnProduct(ownerId, productId);
        apply(product, form);
        List<MultipartFile> files = nonEmpty(newImages);
        if (!files.isEmpty()) {
            int current = product.getImages().size();
            if (current + files.size() > MAX_IMAGES) {
                throw new BusinessException("Mỗi sản phẩm tối đa " + MAX_IMAGES + " ảnh (hiện có " + current + ").");
            }
            int nextOrder = product.getImages().stream().mapToInt(ProductImage::getSortOrder).max().orElse(-1) + 1;
            addImages(product, files, nextOrder);
            if (product.getImages().stream().noneMatch(ProductImage::isThumbnail)) {
                product.getImages().get(0).setThumbnail(true);
            }
        }
    }

    @Override
    @Transactional
    public void setThumbnail(Long ownerId, Long productId, Long imageId) {
        Product product = requireOwnProduct(ownerId, productId);
        ProductImage chosen = requireImage(product, imageId);
        product.getImages().forEach(i -> i.setThumbnail(i == chosen));
    }

    @Override
    @Transactional
    public void deleteImage(Long ownerId, Long productId, Long imageId) {
        Product product = requireOwnProduct(ownerId, productId);
        ProductImage image = requireImage(product, imageId);
        if (product.getImages().size() <= 1) {
            throw new BusinessException("Sản phẩm cần ít nhất 1 ảnh. Hãy thêm ảnh khác trước khi xóa ảnh này.");
        }
        product.getImages().remove(image);
        if (image.isThumbnail()) {
            product.getImages().get(0).setThumbnail(true);
        }
        // Ảnh seed / ảnh cũ có thể không có publicId (null) -> không dùng List.of
        deleteAfterCommit(Collections.singletonList(image.getPublicId()));
    }

    @Override
    @Transactional
    public void toggleActive(Long ownerId, Long productId) {
        Product product = requireOwnProduct(ownerId, productId);
        product.setActive(!product.isActive());
    }

    @Override
    @Transactional
    public boolean delete(Long ownerId, Long productId) {
        Product product = requireOwnProduct(ownerId, productId);
        if (orderItemRepository.existsByProductId(productId)) {
            // Đơn hàng cũ vẫn cần tham chiếu sản phẩm (lịch sử, đánh giá, đối soát) -> chỉ ẩn
            product.setActive(false);
            return false;
        }
        List<String> publicIds = product.getImages().stream().map(ProductImage::getPublicId).toList();
        cartItemRepository.deleteByProductId(productId);
        favoriteRepository.deleteByProductId(productId);
        viewedProductRepository.deleteByProductId(productId);
        productRepository.delete(product);
        deleteAfterCommit(publicIds);
        return true;
    }

    // ------------------------------------------------------------------ helpers

    /** Shop của vendor (đã duyệt hoặc đang bị đình chỉ vẫn được quản lý sản phẩm). */
    private Shop requireShop(Long ownerId) {
        return shopRepository.findByOwnerId(ownerId)
                .filter(s -> s.getStatus() == ShopStatus.APPROVED || s.getStatus() == ShopStatus.SUSPENDED)
                .orElseThrow(() -> new NotFoundException("Bạn chưa có shop đang hoạt động"));
    }

    /** Sản phẩm phải thuộc shop của vendor; của shop khác thì báo không tìm thấy (không lộ thông tin). */
    private Product requireOwnProduct(Long ownerId, Long productId) {
        Shop shop = requireShop(ownerId);
        return productRepository.findById(productId)
                .filter(p -> p.getShop().getId().equals(shop.getId()))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm"));
    }

    private static ProductImage requireImage(Product product, Long imageId) {
        return product.getImages().stream().filter(i -> i.getId().equals(imageId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ảnh"));
    }

    private void apply(Product product, VendorProductForm form) {
        String name = form.getName() == null ? "" : form.getName().trim().replaceAll("\\s+", " ");
        if (name.length() < 3) {
            throw new BusinessException("Tên sản phẩm từ 3 đến 200 ký tự.");
        }
        if (form.getPrice() == null || form.getPrice().signum() <= 0) {
            throw new BusinessException("Giá bán phải lớn hơn 0.");
        }
        if (form.getOriginalPrice() != null && form.getOriginalPrice().compareTo(form.getPrice()) <= 0) {
            throw new BusinessException("Giá gốc phải lớn hơn giá bán (để trống nếu không giảm giá).");
        }
        if (form.getStock() == null || form.getStock() < 0) {
            throw new BusinessException("Tồn kho không được âm.");
        }
        Category category = categoryRepository.findById(form.getCategoryId() == null ? -1L : form.getCategoryId())
                .filter(Category::isActive)
                .orElseThrow(() -> new BusinessException("Danh mục không tồn tại hoặc đang bị ẩn."));
        product.setName(name);
        product.setCategory(category);
        product.setPrice(form.getPrice());
        product.setOriginalPrice(form.getOriginalPrice());
        product.setStock(form.getStock());
        product.setDescription(StringUtils.hasText(form.getDescription()) ? form.getDescription().trim() : null);
        product.setActive(form.isActive());
    }

    private String uniqueSlug(String name) {
        String base = SlugUtil.toSlug(name);
        if (base.isEmpty()) {
            base = "san-pham";
        }
        String candidate = base;
        for (int i = 2; productRepository.existsBySlug(candidate); i++) {
            candidate = base + "-" + i;
        }
        return candidate;
    }

    private static List<MultipartFile> nonEmpty(List<MultipartFile> files) {
        return files == null ? List.of() : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
    }

    /**
     * Upload ảnh và gắn vào sản phẩm. Lỗi giữa chừng -> xóa các ảnh đã upload; transaction rollback sau đó
     * cũng xóa ảnh mới để không bỏ rác trên Cloudinary.
     */
    private void addImages(Product product, List<MultipartFile> files, int startOrder) {
        List<String> uploaded = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                UploadResult result = fileStorageService.uploadImage(file, IMAGE_FOLDER);
                uploaded.add(result.publicId());
                product.getImages().add(ProductImage.builder()
                        .product(product).url(result.url()).publicId(result.publicId())
                        .sortOrder(startOrder++).build());
            }
        } catch (RuntimeException e) {
            uploaded.forEach(this::deleteQuietly);
            throw e;
        }
        deleteOnRollback(uploaded);
    }

    private void deleteOnRollback(List<String> publicIds) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        publicIds.forEach(VendorProductServiceImpl.this::deleteQuietly);
                    }
                }
            });
        }
    }

    private void deleteAfterCommit(List<String> publicIds) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publicIds.forEach(this::deleteQuietly);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publicIds.forEach(VendorProductServiceImpl.this::deleteQuietly);
            }
        });
    }

    private void deleteQuietly(String publicId) {
        if (!StringUtils.hasText(publicId)) {
            return;
        }
        try {
            fileStorageService.delete(publicId, MediaType.IMAGE);
        } catch (FileStorageException e) {
            log.warn("Không xóa được ảnh sản phẩm {}: {}", publicId, e.getMessage());
        }
    }
}
