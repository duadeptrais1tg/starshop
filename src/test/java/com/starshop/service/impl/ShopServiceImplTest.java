package com.starshop.service.impl;

import com.starshop.dto.UploadResult;
import com.starshop.dto.shop.ShopForm;
import com.starshop.dto.shop.ShopPageDto;
import com.starshop.entity.Shop;
import com.starshop.entity.User;
import com.starshop.entity.enums.MediaType;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.InvalidFileException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.ShopRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
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

class ShopServiceImplTest {

    private static final Long USER_ID = 5L;

    private final ShopRepository shopRepository = mock(ShopRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final FileStorageService storage = mock(FileStorageService.class);
    private final ShopServiceImpl service = new ShopServiceImpl(shopRepository, userRepository, productRepository, storage);

    private User owner;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(USER_ID).email("an@gmail.com").fullName("An").phone("0912345678").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(shopRepository.findByOwnerId(USER_ID)).thenReturn(Optional.empty());
        when(shopRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        when(shopRepository.saveAndFlush(any(Shop.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void register_createsPendingShop_withSlugAndNormalizedValues() {
        when(shopRepository.findBySlug("hoa-tuoi-moi-ngay")).thenReturn(Optional.of(Shop.builder().id(99L).build()));

        service.register(USER_ID, form("  Hoa   Tươi Mỗi Ngày "), null, empty("banner"));

        Shop saved = captureSaved();
        assertThat(saved.getOwner()).isSameAs(owner);
        assertThat(saved.getName()).isEqualTo("Hoa Tươi Mỗi Ngày");
        assertThat(saved.getSlug()).isEqualTo("hoa-tuoi-moi-ngay-2");
        assertThat(saved.getStatus()).isEqualTo(ShopStatus.PENDING);
        assertThat(saved.getPhone()).isEqualTo("0987654321");
        assertThat(saved.getDescription()).isNull();
        verify(storage, never()).uploadImage(any(), anyString());
    }

    @Test
    void register_duplicateName_rejected() {
        when(shopRepository.existsNameForOtherShop("Hoa Xinh", null)).thenReturn(true);

        assertThatThrownBy(() -> service.register(USER_ID, form("Hoa Xinh"), null, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("đã được sử dụng");
        verify(shopRepository, never()).saveAndFlush(any());
    }

    @Test
    void register_whenPendingOrApproved_rejected() {
        Shop pending = shop(ShopStatus.PENDING);
        when(shopRepository.findByOwnerId(USER_ID)).thenReturn(Optional.of(pending));
        assertThatThrownBy(() -> service.register(USER_ID, form("Shop Mới"), null, null))
                .hasMessageContaining("chờ quản trị viên duyệt");

        pending.setStatus(ShopStatus.APPROVED);
        assertThatThrownBy(() -> service.register(USER_ID, form("Shop Mới"), null, null))
                .hasMessageContaining("đã có shop");
    }

    @Test
    void register_afterRejection_resubmitsAsPending() {
        Shop rejected = shop(ShopStatus.REJECTED);
        rejected.setStatusReason("Thiếu thông tin");
        when(shopRepository.findByOwnerId(USER_ID)).thenReturn(Optional.of(rejected));

        service.register(USER_ID, form("Hoa Xinh Mới"), null, null);

        assertThat(rejected.getStatus()).isEqualTo(ShopStatus.PENDING);
        assertThat(rejected.getStatusReason()).isNull();
        assertThat(rejected.getSlug()).isEqualTo("hoa-xinh-moi");
        verify(shopRepository).existsNameForOtherShop("Hoa Xinh Mới", 1L);
    }

    @Test
    void register_withLogo_uploadsAndStoresPublicId() {
        when(storage.uploadImage(any(), eq("shops/logos")))
                .thenReturn(new UploadResult("https://cdn/logo.png", "shops/logos/abc", MediaType.IMAGE));

        service.register(USER_ID, form("Hoa Xinh"), image("logo"), null);

        Shop saved = captureSaved();
        assertThat(saved.getLogoUrl()).isEqualTo("https://cdn/logo.png");
        assertThat(saved.getLogoPublicId()).isEqualTo("shops/logos/abc");
    }

    @Test
    void register_bannerUploadFails_deletesLogoJustUploaded() {
        when(storage.uploadImage(any(), eq("shops/logos")))
                .thenReturn(new UploadResult("https://cdn/logo.png", "shops/logos/abc", MediaType.IMAGE));
        when(storage.uploadImage(any(), eq("shops/banners"))).thenThrow(new InvalidFileException("Ảnh tối đa 5MB"));

        assertThatThrownBy(() -> service.register(USER_ID, form("Hoa Xinh"), image("logo"), image("banner")))
                .isInstanceOf(InvalidFileException.class);
        verify(storage).delete("shops/logos/abc", MediaType.IMAGE);
        verify(shopRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_approvedShop_keepsSlug_andReplacesBanner() {
        Shop shop = shop(ShopStatus.APPROVED);
        shop.setBannerPublicId("old-banner");
        when(shopRepository.findByOwnerId(USER_ID)).thenReturn(Optional.of(shop));
        when(storage.uploadImage(any(), eq("shops/banners")))
                .thenReturn(new UploadResult("https://cdn/new.png", "new-banner", MediaType.IMAGE));

        service.update(USER_ID, form("Tên Hoàn Toàn Mới"), null, image("banner"));

        assertThat(shop.getName()).isEqualTo("Tên Hoàn Toàn Mới");
        assertThat(shop.getSlug()).isEqualTo("hoa-xinh");
        assertThat(shop.getBannerUrl()).isEqualTo("https://cdn/new.png");
        // Không có transaction trong unit test: ảnh cũ bị xóa ngay
        verify(storage).delete("old-banner", MediaType.IMAGE);
        verify(shopRepository).existsNameForOtherShop("Tên Hoàn Toàn Mới", 1L);
    }

    @Test
    void update_shopNotApproved_notFound() {
        when(shopRepository.findByOwnerId(USER_ID)).thenReturn(Optional.of(shop(ShopStatus.PENDING)));

        assertThatThrownBy(() -> service.update(USER_ID, form("Hoa Xinh"), null, null)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void publicPage_onlyApproved_andWeightedRating() {
        Shop shop = shop(ShopStatus.APPROVED);
        when(shopRepository.findBySlug("hoa-xinh")).thenReturn(Optional.of(shop));
        ProductRepository.ShopStats stats = mock(ProductRepository.ShopStats.class);
        when(stats.getProductCount()).thenReturn(3L);
        when(stats.getSoldCount()).thenReturn(40L);
        when(stats.getReviewCount()).thenReturn(3L);
        // 2 lượt 5 sao + 1 lượt 4 sao
        when(stats.getRatingSum()).thenReturn(new BigDecimal("14.00"));
        when(productRepository.shopStats(1L)).thenReturn(stats);

        ShopPageDto page = service.getPublicPage("hoa-xinh");

        assertThat(page.getRatingAvg()).isEqualByComparingTo("4.7");
        assertThat(page.getRatingRounded()).isEqualTo(4.5);
        assertThat(page.getProductCount()).isEqualTo(3);

        shop.setStatus(ShopStatus.SUSPENDED);
        assertThatThrownBy(() -> service.getPublicPage("hoa-xinh")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.getPublicPage("khong-co")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getForm_prefillsUserPhoneForFirstRegistration() {
        assertThat(service.getForm(USER_ID).getPhone()).isEqualTo("0912345678");
        assertThat(service.findMyShop(USER_ID)).isNull();
    }

    private Shop captureSaved() {
        ArgumentCaptor<Shop> captor = ArgumentCaptor.forClass(Shop.class);
        verify(shopRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private Shop shop(ShopStatus status) {
        return Shop.builder().id(1L).owner(owner).name("Hoa Xinh").slug("hoa-xinh")
                .pickupAddress("1 Lê Lợi").phone("0912345678").status(status).build();
    }

    private static ShopForm form(String name) {
        ShopForm form = new ShopForm();
        form.setName(name);
        form.setDescription("   ");
        form.setPickupAddress(" 12 Nguyễn Huệ, Q1, TP.HCM ");
        form.setPhone("+84987654321");
        return form;
    }

    private static MockMultipartFile image(String name) {
        return new MockMultipartFile(name, name + ".png", "image/png", new byte[]{1, 2, 3});
    }

    private static MockMultipartFile empty(String name) {
        return new MockMultipartFile(name, "", "application/octet-stream", new byte[0]);
    }
}
