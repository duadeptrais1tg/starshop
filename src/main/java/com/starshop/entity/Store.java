package com.starshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Chi nhánh trong chuỗi StarShop. Manager được gán cho một Store,
 * các Shop thuộc Store được Admin gán khi duyệt shop.
 */
@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Store extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(length = 15)
    private String phone;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
