package com.starshop.config;

import com.starshop.entity.Carrier;
import com.starshop.entity.Category;
import com.starshop.entity.Product;
import com.starshop.entity.ProductImage;
import com.starshop.entity.Role;
import com.starshop.entity.Shop;
import com.starshop.entity.ShopCommission;
import com.starshop.entity.Store;
import com.starshop.entity.User;
import com.starshop.entity.enums.RoleName;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.repository.CarrierRepository;
import com.starshop.repository.CategoryRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.RoleRepository;
import com.starshop.repository.ShopCommissionRepository;
import com.starshop.repository.ShopRepository;
import com.starshop.repository.StoreRepository;
import com.starshop.repository.UserRepository;
import com.starshop.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tạo dữ liệu mẫu khi chạy profile local. Chỉ chạy khi bảng users còn trống,
 * nên khởi động lại nhiều lần không bị nhân đôi dữ liệu.
 * Mật khẩu mọi tài khoản mẫu: {@value #DEFAULT_PASSWORD}
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "Starshop@123";
    private static final String IMAGE_URL = "https://images.unsplash.com/photo-%s?w=600&h=600&fit=crop&auto=format&q=80";

    /** Mã hóa BCrypt, cùng thuật toán với PasswordEncoder của phần bảo mật. */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final CarrierRepository carrierRepository;
    private final ShopRepository shopRepository;
    private final ShopCommissionRepository shopCommissionRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Map<RoleName, Role> roles = seedRoles();
        if (userRepository.count() > 0) {
            log.info("[DataSeeder] Đã có dữ liệu, bỏ qua tạo dữ liệu mẫu.");
            return;
        }

        List<Store> stores = seedStores();
        List<Carrier> carriers = seedCarriers();

        createUser("Quản trị viên", "admin@starshop.vn", "0900000001", Set.of(roles.get(RoleName.ADMIN)), null, null);
        createUser("Quản lý Quận 1", "manager@starshop.vn", "0900000002", Set.of(roles.get(RoleName.MANAGER)), stores.get(0), null);
        User vendor = createUser("Chủ shop Ánh Sao", "vendor@starshop.vn", "0900000003",
                Set.of(roles.get(RoleName.USER), roles.get(RoleName.VENDOR)), null, null);
        createUser("Shipper GHN", "shipper@starshop.vn", "0900000004", Set.of(roles.get(RoleName.SHIPPER)), null, carriers.get(0));
        createUser("Nguyễn Văn Khách", "user@starshop.vn", "0900000005", Set.of(roles.get(RoleName.USER)), null, null);

        Shop shop = seedShop(vendor, stores.get(0));
        seedCommission();
        Map<String, Category> categories = seedCategories();
        seedProducts(shop, categories);

        log.info("[DataSeeder] Đã tạo dữ liệu mẫu. Mật khẩu các tài khoản: {}", DEFAULT_PASSWORD);
    }

    private Map<RoleName, Role> seedRoles() {
        Map<RoleName, Role> roles = new HashMap<>();
        for (RoleName name : RoleName.values()) {
            Role role = roleRepository.findByName(name)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
            roles.put(name, role);
        }
        return roles;
    }

    private List<Store> seedStores() {
        return storeRepository.saveAll(List.of(
                Store.builder().name("StarShop Quận 1")
                        .address("12 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh")
                        .phone("02838220001").build(),
                Store.builder().name("StarShop Thủ Đức")
                        .address("1 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh")
                        .phone("02838960002").build()));
    }

    private List<Carrier> seedCarriers() {
        return carrierRepository.saveAll(List.of(
                Carrier.builder().name("Giao Hàng Nhanh").shippingFee(new BigDecimal("30000")).build(),
                Carrier.builder().name("Giao Hàng Tiết Kiệm").shippingFee(new BigDecimal("25000")).build(),
                Carrier.builder().name("Viettel Post").shippingFee(new BigDecimal("28000")).build()));
    }

    private User createUser(String fullName, String email, String phone, Set<Role> roles, Store store, Carrier carrier) {
        return userRepository.save(User.builder()
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .enabled(true)
                .roles(new HashSet<>(roles))
                .store(store)
                .carrier(carrier)
                .build());
    }

    private Shop seedShop(User owner, Store store) {
        String name = "Hoa Tươi Ánh Sao";
        return shopRepository.save(Shop.builder()
                .owner(owner)
                .store(store)
                .name(name)
                .slug(SlugUtil.toSlug(name))
                .description("Tiệm hoa tươi thiết kế theo yêu cầu, giao nhanh trong 2 giờ tại TP. Hồ Chí Minh.")
                .logoUrl(IMAGE_URL.formatted("1487530811176-3780de880c2d"))
                .bannerUrl(IMAGE_URL.formatted("1455659817273-f96807779a8a"))
                .pickupAddress("45 Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh")
                .phone("0900000003")
                .status(ShopStatus.APPROVED)
                .build());
    }

    /** Mức chiết khấu mặc định toàn sàn 10% (shop = null). */
    private void seedCommission() {
        shopCommissionRepository.save(ShopCommission.builder()
                .rate(new BigDecimal("10.00"))
                .effectiveFrom(LocalDate.now().withDayOfYear(1))
                .build());
    }

    private Map<String, Category> seedCategories() {
        List<Category> list = categoryRepository.saveAll(List.of(
                category("Hoa sinh nhật", "1526047932273-341f2a7631f9"),
                category("Hoa khai trương", "1470509037663-253afd7f0f51"),
                category("Hoa cưới", "1523694576729-dc99e9c0f9b4"),
                category("Hoa tình yêu", "1496062031456-07b8f162a322"),
                category("Hoa chia buồn", "1560717789-0ac7c58ac90a"),
                category("Hoa để bàn", "1561181286-d3fee7d55364")));
        return list.stream().collect(Collectors.toMap(Category::getSlug, c -> c));
    }

    private Category category(String name, String imageId) {
        return Category.builder().name(name).slug(SlugUtil.toSlug(name)).imageUrl(IMAGE_URL.formatted(imageId)).build();
    }

    /**
     * ~30 sản phẩm mẫu. soldCount được đặt sẵn để có dữ liệu cho trang chủ "bán chạy"
     * (thực tế cột này tăng khi đơn được giao thành công).
     */
    private void seedProducts(Shop shop, Map<String, Category> categories) {
        List<ProductSeed> seeds = List.of(
                new ProductSeed("Bó hoa hồng đỏ 20 bông", "hoa-tinh-yeu", 550_000, 650_000L, 40, 86, "1496062031456-07b8f162a322", "1519378058457-4c29a0a2efac"),
                new ProductSeed("Hoa hồng cầu vồng", "hoa-tinh-yeu", 890_000, null, 15, 23, "1508610048659-a06b669e3321"),
                new ProductSeed("Bình hồng pastel ngọt ngào", "hoa-tinh-yeu", 720_000, 800_000L, 20, 41, "1591886960571-74d43a9d4166"),
                new ProductSeed("Hồng đơn trong bình thủy tinh", "hoa-tinh-yeu", 250_000, null, 50, 12, "1518895949257-7621c3c786d7"),
                new ProductSeed("Trái tim yêu thương", "hoa-tinh-yeu", 980_000, 1_100_000L, 10, 18, "1526047932273-341f2a7631f9"),
                new ProductSeed("Mẫu đơn hồng Pháp", "hoa-tinh-yeu", 1_250_000, null, 8, 7, "1582794543139-8ac9cb0f7b11"),

                new ProductSeed("Bó hoa sinh nhật rực rỡ", "hoa-sinh-nhat", 480_000, 550_000L, 35, 64, "1487530811176-3780de880c2d"),
                new ProductSeed("Giỏ hoa mừng tuổi mới", "hoa-sinh-nhat", 650_000, null, 20, 29, "1533616688419-b7a585564566"),
                new ProductSeed("Bó hoa pastel tinh khôi", "hoa-sinh-nhat", 520_000, null, 25, 33, "1563241527-3004b7be0ffd"),
                new ProductSeed("Bó hoa cầm tay Sweetie", "hoa-sinh-nhat", 390_000, 450_000L, 30, 52, "1567696153798-9111f9cd3d0d"),
                new ProductSeed("Hoa sinh nhật Thược dược", "hoa-sinh-nhat", 430_000, null, 18, 9, "1444021465936-c6ca81d39b84"),
                new ProductSeed("Bó hoa sắc đỏ nồng nàn", "hoa-sinh-nhat", 590_000, null, 22, 15, "1457089328109-e5d9bd499191"),

                new ProductSeed("Kệ hoa hướng dương khai trương", "hoa-khai-truong", 1_500_000, 1_750_000L, 10, 27, "1455659817273-f96807779a8a"),
                new ProductSeed("Lẵng hướng dương phát tài", "hoa-khai-truong", 1_200_000, null, 12, 19, "1470509037663-253afd7f0f51"),
                new ProductSeed("Kệ hoa hồng thịnh vượng", "hoa-khai-truong", 1_800_000, null, 6, 11, "1519378058457-4c29a0a2efac"),
                new ProductSeed("Lẵng hoa vàng may mắn", "hoa-khai-truong", 990_000, 1_150_000L, 14, 8, "1490750967868-88aa4486c946"),
                new ProductSeed("Kệ hoa mix hồng cam", "hoa-khai-truong", 1_350_000, null, 9, 5, "1533616688419-b7a585564566"),

                new ProductSeed("Hoa cưới cầm tay trắng tinh khôi", "hoa-cuoi", 850_000, null, 15, 21, "1523694576729-dc99e9c0f9b4"),
                new ProductSeed("Hoa cưới mẫu đơn hồng", "hoa-cuoi", 1_450_000, 1_600_000L, 6, 13, "1582794543139-8ac9cb0f7b11"),
                new ProductSeed("Hoa cưới pastel vintage", "hoa-cuoi", 950_000, null, 10, 6, "1563241527-3004b7be0ffd"),
                new ProductSeed("Hoa cưới hồng kem", "hoa-cuoi", 1_100_000, null, 8, 4, "1591886960571-74d43a9d4166"),

                new ProductSeed("Kệ hoa chia buồn cúc trắng", "hoa-chia-buon", 1_300_000, null, 10, 14, "1560717789-0ac7c58ac90a"),
                new ProductSeed("Vòng hoa tiễn biệt thanh khiết", "hoa-chia-buon", 1_600_000, null, 5, 3, "1523694576729-dc99e9c0f9b4"),
                new ProductSeed("Lẵng ly trắng thành kính", "hoa-chia-buon", 1_150_000, null, 7, 2, "1502977249166-824b3a8a4d6d"),

                new ProductSeed("Bình tulip hồng để bàn", "hoa-de-ban", 690_000, 750_000L, 20, 38, "1561181286-d3fee7d55364"),
                new ProductSeed("Bình hoa ly hồng", "hoa-de-ban", 560_000, null, 16, 17, "1502977249166-824b3a8a4d6d"),
                new ProductSeed("Ly đơn tinh tế", "hoa-de-ban", 210_000, null, 40, 10, "1525310072745-f49212b5ac6d"),
                new ProductSeed("Bình cúc họa mi", "hoa-de-ban", 320_000, null, 30, 26, "1560717789-0ac7c58ac90a"),
                new ProductSeed("Bình hướng dương mini", "hoa-de-ban", 290_000, 350_000L, 25, 31, "1470509037663-253afd7f0f51"),
                new ProductSeed("Bình hoa mix mùa hè", "hoa-de-ban", 610_000, null, 0, 45, "1490750967868-88aa4486c946"));

        for (ProductSeed seed : seeds) {
            Product product = Product.builder()
                    .shop(shop)
                    .category(categories.get(seed.categorySlug()))
                    .name(seed.name())
                    .slug(SlugUtil.toSlug(seed.name()))
                    .description(seed.name() + " – được cắm từ hoa tươi nhập trong ngày, "
                            + "kèm thiệp chúc mừng miễn phí. Giao nhanh nội thành TP. Hồ Chí Minh.")
                    .price(BigDecimal.valueOf(seed.price()))
                    .originalPrice(seed.originalPrice() == null ? null : BigDecimal.valueOf(seed.originalPrice()))
                    .stock(seed.stock())
                    .soldCount(seed.sold())
                    .build();
            for (int i = 0; i < seed.imageIds().length; i++) {
                product.addImage(ProductImage.builder()
                        .url(IMAGE_URL.formatted(seed.imageIds()[i]))
                        .thumbnail(i == 0)
                        .sortOrder(i)
                        .build());
            }
            productRepository.save(product);
        }
        log.info("[DataSeeder] Đã tạo {} sản phẩm.", seeds.size());
    }

    private record ProductSeed(String name, String categorySlug, long price, Long originalPrice,
                               int stock, int sold, String... imageIds) {
    }
}
