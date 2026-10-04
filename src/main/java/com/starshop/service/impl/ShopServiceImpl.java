package com.starshop.service.impl;

import com.starshop.dto.UploadResult;
import com.starshop.dto.shop.MyShopDto;
import com.starshop.dto.shop.ShopForm;
import com.starshop.dto.shop.ShopPageDto;
import com.starshop.entity.Shop;
import com.starshop.entity.User;
import com.starshop.entity.enums.MediaType;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.FileStorageException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.ShopRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.FileStorageService;
import com.starshop.service.ShopService;
import com.starshop.util.DateFormats;
import com.starshop.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private static final String LOGO_FOLDER = "shops/logos";
    private static final String BANNER_FOLDER = "shops/banners";
    private static final DateTimeFormatter JOINED_FORMAT = DateTimeFormatter.ofPattern("MM/yyyy");

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public MyShopDto findMyShop(Long userId) {
        return shopRepository.findByOwnerId(userId).map(ShopServiceImpl::toMyShop).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopForm getForm(Long userId) {
        ShopForm form = new ShopForm();
        shopRepository.findByOwnerId(userId).ifPresentOrElse(shop -> {
            form.setName(shop.getName());
            form.setDescription(shop.getDescription());
            form.setPickupAddress(shop.getPickupAddress());
            form.setPhone(shop.getPhone());
        }, () -> userRepository.findById(userId).ifPresent(user -> form.setPhone(user.getPhone())));
        return form;
    }

    @Override
    @Transactional
    public void register(Long userId, ShopForm form, MultipartFile logo, MultipartFile banner) {
        Shop shop = shopRepository.findByOwnerId(userId).orElse(null);
        if (shop != null && shop.getStatus() != ShopStatus.REJECTED) {
            throw new BusinessException(shop.getStatus() == ShopStatus.PENDING
                    ? "Bạn đã gửi yêu cầu mở shop, vui lòng chờ quản trị viên duyệt."
                    : "Bạn đã có shop, không thể đăng ký thêm.");
        }
        String name = normalizeName(form.getName());
        requireUniqueName(name, shop == null ? null : shop.getId());

        if (shop == null) {
            User owner = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản"));
            shop = Shop.builder().owner(owner).build();
        }
        // Shop chưa từng được duyệt nên chưa ai biết đường dẫn: tạo slug theo tên mới
        if (shop.getSlug() == null || !shop.getName().equals(name)) {
            shop.setSlug(uniqueSlug(name, shop.getId()));
        }
        shop.setName(name);
        applyInfo(shop, form);
        shop.setStatus(ShopStatus.PENDING);
        shop.setStatusReason(null);
        applyImages(shop, logo, banner);
        save(shop);
    }

    @Override
    @Transactional
    public void update(Long ownerId, ShopForm form, MultipartFile logo, MultipartFile banner) {
        Shop shop = shopRepository.findByOwnerId(ownerId)
                .filter(s -> s.getStatus() == ShopStatus.APPROVED || s.getStatus() == ShopStatus.SUSPENDED)
                .orElseThrow(() -> new NotFoundException("Bạn chưa có shop đang hoạt động"));
        String name = normalizeName(form.getName());
        requireUniqueName(name, shop.getId());
        shop.setName(name);
        applyInfo(shop, form);
        applyImages(shop, logo, banner);
        save(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopPageDto getPublicPage(String slug) {
        Shop shop = shopRepository.findBySlug(slug)
                .filter(s -> s.getStatus() == ShopStatus.APPROVED)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy shop"));
        ProductRepository.ShopStats stats = productRepository.shopStats(shop.getId());
        BigDecimal rating = stats.getReviewCount() == 0 ? BigDecimal.ZERO
                : stats.getRatingSum().divide(BigDecimal.valueOf(stats.getReviewCount()), 1, RoundingMode.HALF_UP);
        return ShopPageDto.builder()
                .id(shop.getId())
                .name(shop.getName())
                .slug(shop.getSlug())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .bannerUrl(shop.getBannerUrl())
                .joinedAt(shop.getCreatedAt() == null ? null : shop.getCreatedAt().format(JOINED_FORMAT))
                .productCount(stats.getProductCount())
                .soldCount(stats.getSoldCount())
                .reviewCount(stats.getReviewCount())
                .ratingAvg(rating)
                .build();
    }

    // ------------------------------------------------------------------ helpers

    private static MyShopDto toMyShop(Shop shop) {
        return MyShopDto.builder()
                .id(shop.getId())
                .name(shop.getName())
                .slug(shop.getSlug())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .bannerUrl(shop.getBannerUrl())
                .pickupAddress(shop.getPickupAddress())
                .phone(shop.getPhone())
                .status(shop.getStatus())
                .statusReason(shop.getStatusReason())
                .storeName(shop.getStore() == null ? null : shop.getStore().getName())
                .createdAt(DateFormats.dateTime(shop.getCreatedAt()))
                .build();
    }

    private static void applyInfo(Shop shop, ShopForm form) {
        shop.setDescription(StringUtils.hasText(form.getDescription()) ? form.getDescription().trim() : null);
        shop.setPickupAddress(form.getPickupAddress().trim());
        shop.setPhone(normalizePhone(form.getPhone()));
    }

    /** Gộp khoảng trắng thừa, để "Hoa  Xinh" và "Hoa Xinh" được coi là trùng tên. */
    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (normalized.length() < 3 || normalized.length() > ShopForm.MAX_NAME_LENGTH) {
            throw new BusinessException("Tên shop từ 3 đến 150 ký tự.");
        }
        return normalized;
    }

    private void requireUniqueName(String name, Long excludeId) {
        if (shopRepository.existsNameForOtherShop(name, excludeId)) {
            throw new BusinessException("Tên shop \"" + name + "\" đã được sử dụng, vui lòng chọn tên khác.");
        }
    }

    private String uniqueSlug(String name, Long shopId) {
        String base = SlugUtil.toSlug(name);
        if (base.isEmpty()) {
            base = "shop";
        }
        String candidate = base;
        for (int i = 2; shopRepository.findBySlug(candidate).filter(s -> !s.getId().equals(shopId)).isPresent(); i++) {
            candidate = base + "-" + i;
        }
        return candidate;
    }

    private static String normalizePhone(String phone) {
        String trimmed = phone.trim();
        return trimmed.startsWith("+84") ? "0" + trimmed.substring(3) : trimmed;
    }

    /**
     * Upload logo / banner mới (nếu có). Ảnh cũ chỉ bị xóa khi lưu DB thành công;
     * lưu thất bại thì xóa ảnh vừa upload để không bỏ rác trên Cloudinary.
     */
    private void applyImages(Shop shop, MultipartFile logo, MultipartFile banner) {
        UploadResult newLogo = upload(logo, LOGO_FOLDER);
        UploadResult newBanner;
        try {
            newBanner = upload(banner, BANNER_FOLDER);
        } catch (RuntimeException e) {
            if (newLogo != null) {
                deleteQuietly(newLogo.publicId());
            }
            throw e;
        }
        if (newLogo != null) {
            cleanupAfterCompletion(shop.getLogoPublicId(), newLogo.publicId());
            shop.setLogoUrl(newLogo.url());
            shop.setLogoPublicId(newLogo.publicId());
        }
        if (newBanner != null) {
            cleanupAfterCompletion(shop.getBannerPublicId(), newBanner.publicId());
            shop.setBannerUrl(newBanner.url());
            shop.setBannerPublicId(newBanner.publicId());
        }
    }

    private UploadResult upload(MultipartFile file, String folder) {
        return file == null || file.isEmpty() ? null : fileStorageService.uploadImage(file, folder);
    }

    /** Lưu ngay để bắt lỗi trùng tên / slug (hai người đăng ký cùng lúc) thành thông báo dễ hiểu. */
    private void save(Shop shop) {
        try {
            shopRepository.saveAndFlush(shop);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Tên shop đã được sử dụng, vui lòng chọn tên khác.");
        }
    }

    private void cleanupAfterCompletion(String oldPublicId, String newPublicId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(oldPublicId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                deleteQuietly(status == STATUS_COMMITTED ? oldPublicId : newPublicId);
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
            log.warn("Không xóa được ảnh shop {}: {}", publicId, e.getMessage());
        }
    }
}
