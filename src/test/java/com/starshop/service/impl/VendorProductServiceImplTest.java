package com.starshop.service.impl;

import com.starshop.dto.UploadResult;
import com.starshop.dto.product.VendorProductForm;
import com.starshop.entity.Category;
import com.starshop.entity.Product;
import com.starshop.entity.ProductImage;
import com.starshop.entity.Shop;
import com.starshop.entity.enums.MediaType;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.InvalidFileException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.CartItemRepository;
import com.starshop.repository.CategoryRepository;
import com.starshop.repository.FavoriteRepository;
import com.starshop.repository.OrderItemRepository;
import com.starshop.repository.ProductImageRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.ShopRepository;
import com.starshop.repository.ViewedProductRepository;
import com.starshop.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VendorProductServiceImplTest {

    private static final Long OWNER_ID = 3L;

    private final ShopRepository shopRepository = mock(ShopRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
    private final CartItemRepository cartItemRepository = mock(CartItemRepository.class);
    private final FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
    private final ViewedProductRepository viewedRepository = mock(ViewedProductRepository.class);
    private final FileStorageService storage = mock(FileStorageService.class);
    private final VendorProductServiceImpl service = new VendorProductServiceImpl(shopRepository, productRepository,
            mock(ProductImageRepository.class), categoryRepository, orderItemRepository, cartItemRepository,
            favoriteRepository, viewedRepository, storage);

    private Shop myShop;
    private Shop otherShop;
    private Category category;

    @BeforeEach
    void setUp() {
        myShop = Shop.builder().id(1L).name("Shop tôi").status(ShopStatus.APPROVED).build();
        otherShop = Shop.builder().id(2L).name("Shop khác").status(ShopStatus.APPROVED).build();
        category = Category.builder().id(5L).name("Hoa hồng").active(true).build();
        when(shopRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(myShop));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });
        AtomicInteger n = new AtomicInteger();
        when(storage.uploadImage(any(), eq("products"))).thenAnswer(inv -> {
            int i = n.incrementAndGet();
            return new UploadResult("https://cdn/img" + i + ".png", "products/img" + i, MediaType.IMAGE);
        });
    }

    @Test
    void create_savesProductWithImages_firstIsThumbnail_uniqueSlug() {
        when(productRepository.existsBySlug("bo-hoa-hong-do")).thenReturn(true);

        Long id = service.create(OWNER_ID, form("  Bó hoa   hồng đỏ "), List.of(image(), image()));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();
        assertThat(id).isEqualTo(100L);
        assertThat(saved.getShop()).isSameAs(myShop);
        assertThat(saved.getName()).isEqualTo("Bó hoa hồng đỏ");
        assertThat(saved.getSlug()).isEqualTo("bo-hoa-hong-do-2");
        assertThat(saved.getImages()).hasSize(2);
        assertThat(saved.getImages().get(0).isThumbnail()).isTrue();
        assertThat(saved.getImages().get(1).isThumbnail()).isFalse();
        assertThat(saved.getImages().get(1).getSortOrder()).isEqualTo(1);
    }

    @Test
    void create_validatesImagesAndValues() {
        assertThatThrownBy(() -> service.create(OWNER_ID, form("Hoa đẹp"), List.of(emptyFile())))
                .hasMessageContaining("ít nhất 1 ảnh");

        VendorProductForm badOriginal = form("Hoa đẹp");
        badOriginal.setOriginalPrice(new BigDecimal("100000"));   // không lớn hơn giá bán 200k
        assertThatThrownBy(() -> service.create(OWNER_ID, badOriginal, List.of(image())))
                .hasMessageContaining("Giá gốc phải lớn hơn giá bán");

        category.setActive(false);
        assertThatThrownBy(() -> service.create(OWNER_ID, form("Hoa đẹp"), List.of(image())))
                .hasMessageContaining("Danh mục");

        List<MultipartFile> nine = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            nine.add(image());
        }
        assertThatThrownBy(() -> service.create(OWNER_ID, form("Hoa đẹp"), nine)).hasMessageContaining("tối đa 8");
        verify(storage, never()).uploadImage(any(), anyString());
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_uploadFailsMidway_deletesImagesAlreadyUploaded() {
        when(storage.uploadImage(any(), eq("products")))
                .thenReturn(new UploadResult("https://cdn/a.png", "products/a", MediaType.IMAGE))
                .thenThrow(new InvalidFileException("Ảnh tối đa 5MB"));

        assertThatThrownBy(() -> service.create(OWNER_ID, form("Hoa đẹp"), List.of(image(), image())))
                .isInstanceOf(InvalidFileException.class);
        verify(storage).delete("products/a", MediaType.IMAGE);
        verify(productRepository, never()).save(any());
    }

    @Test
    void otherShopsProduct_isNotFound_forEveryAction() {
        Product foreign = product(otherShop);
        when(productRepository.findById(50L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.getForm(OWNER_ID, 50L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.update(OWNER_ID, 50L, form("Đổi tên"), List.of())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.toggleActive(OWNER_ID, 50L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(OWNER_ID, 50L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.setThumbnail(OWNER_ID, 50L, 1L)).isInstanceOf(NotFoundException.class);
        assertThat(foreign.getName()).isEqualTo("Hoa cũ");
        assertThat(foreign.isActive()).isTrue();
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void vendorWithoutApprovedShop_cannotManage() {
        myShop.setStatus(ShopStatus.PENDING);
        assertThatThrownBy(() -> service.create(OWNER_ID, form("Hoa đẹp"), List.of(image())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_productWithOrders_isOnlyHidden() {
        Product p = product(myShop);
        when(productRepository.findById(50L)).thenReturn(Optional.of(p));
        when(orderItemRepository.existsByProductId(50L)).thenReturn(true);

        assertThat(service.delete(OWNER_ID, 50L)).isFalse();
        assertThat(p.isActive()).isFalse();
        verify(productRepository, never()).delete(any(Product.class));
        verify(storage, never()).delete(anyString(), any());
    }

    @Test
    void delete_productWithoutOrders_removesReferencesAndImages() {
        Product p = product(myShop);
        p.getImages().add(ProductImage.builder().id(1L).product(p).url("u").publicId("products/x").build());
        when(productRepository.findById(50L)).thenReturn(Optional.of(p));

        assertThat(service.delete(OWNER_ID, 50L)).isTrue();
        verify(cartItemRepository).deleteByProductId(50L);
        verify(favoriteRepository).deleteByProductId(50L);
        verify(viewedRepository).deleteByProductId(50L);
        verify(productRepository).delete(p);
        verify(storage).delete("products/x", MediaType.IMAGE);   // không có transaction trong unit test -> xóa ngay
    }

    @Test
    void images_thumbnailAndDelete_keepAtLeastOne() {
        Product p = product(myShop);
        ProductImage a = ProductImage.builder().id(1L).product(p).url("a").publicId("pa").thumbnail(true).build();
        ProductImage b = ProductImage.builder().id(2L).product(p).url("b").publicId("pb").sortOrder(1).build();
        p.getImages().addAll(List.of(a, b));
        when(productRepository.findById(50L)).thenReturn(Optional.of(p));

        service.setThumbnail(OWNER_ID, 50L, 2L);
        assertThat(a.isThumbnail()).isFalse();
        assertThat(b.isThumbnail()).isTrue();

        service.deleteImage(OWNER_ID, 50L, 2L);
        assertThat(p.getImages()).containsExactly(a);
        assertThat(a.isThumbnail()).isTrue();           // xóa ảnh đại diện -> ảnh còn lại thành đại diện
        verify(storage).delete("pb", MediaType.IMAGE);

        assertThatThrownBy(() -> service.deleteImage(OWNER_ID, 50L, 1L)).isInstanceOf(BusinessException.class);

        // Ảnh không có publicId (dữ liệu mẫu) vẫn xóa được
        p.getImages().add(ProductImage.builder().id(3L).product(p).url("c").publicId(null).sortOrder(2).build());
        service.deleteImage(OWNER_ID, 50L, 3L);
        assertThat(p.getImages()).containsExactly(a);
        assertThatThrownBy(() -> service.setThumbnail(OWNER_ID, 50L, 99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_changesValues_keepsSlug_addsImagesWithinLimit() {
        Product p = product(myShop);
        p.getImages().add(ProductImage.builder().id(1L).product(p).url("a").thumbnail(true).build());
        when(productRepository.findById(50L)).thenReturn(Optional.of(p));
        VendorProductForm form = form("Tên mới hoàn toàn");
        form.setStock(0);
        form.setActive(false);

        service.update(OWNER_ID, 50L, form, List.of(image()));

        assertThat(p.getName()).isEqualTo("Tên mới hoàn toàn");
        assertThat(p.getSlug()).isEqualTo("hoa-cu");
        assertThat(p.getStock()).isZero();
        assertThat(p.isActive()).isFalse();
        assertThat(p.getImages()).hasSize(2);
        assertThat(p.getImages().get(1).getSortOrder()).isEqualTo(1);
        verify(storage, times(1)).uploadImage(any(), eq("products"));
    }

    private Product product(Shop shop) {
        return Product.builder().id(50L).shop(shop).category(category).name("Hoa cũ").slug("hoa-cu")
                .price(new BigDecimal("150000")).stock(10).active(true).build();
    }

    private static VendorProductForm form(String name) {
        VendorProductForm form = new VendorProductForm();
        form.setName(name);
        form.setCategoryId(5L);
        form.setPrice(new BigDecimal("200000"));
        form.setStock(20);
        form.setDescription("  ");
        return form;
    }

    private static MockMultipartFile image() {
        return new MockMultipartFile("images", "a.png", "image/png", new byte[]{1, 2, 3});
    }

    private static MockMultipartFile emptyFile() {
        return new MockMultipartFile("images", "", "application/octet-stream", new byte[0]);
    }
}
