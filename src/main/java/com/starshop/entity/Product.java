package com.starshop.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Sản phẩm hoa của một shop.
 * Các cột soldCount, ratingAvg, reviewCount, favoriteCount được lưu sẵn để sắp xếp nhanh,
 * service cập nhật khi đơn giao thành công / có đánh giá / bấm thích.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Giá bán (> 0). */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    /** Giá gốc để hiển thị gạch ngang (tùy chọn). */
    @Column(name = "original_price", precision = 15, scale = 2)
    private BigDecimal originalPrice;

    @Builder.Default
    @Column(nullable = false)
    private int stock = 0;

    /** false = ngừng bán / bị ẩn. */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "sold_count", nullable = false)
    private int soldCount = 0;

    @Builder.Default
    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @Builder.Default
    @Column(name = "favorite_count", nullable = false)
    private int favoriteCount = 0;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    public void addImage(ProductImage image) {
        image.setProduct(this);
        images.add(image);
    }
}
