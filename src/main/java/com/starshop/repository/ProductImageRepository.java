package com.starshop.repository;

import com.starshop.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /**
     * Ảnh của nhiều sản phẩm trong 1 query (dùng cho danh sách sản phẩm, tránh N+1).
     * Sắp xếp ảnh đại diện lên trước, sau đó theo thứ tự; service chọn ảnh đầu tiên của mỗi sản phẩm.
     * Mỗi phần tử: [productId (Long), url (String)].
     */
    @Query("select i.product.id, i.url from ProductImage i where i.product.id in :productIds "
            + "order by i.product.id, i.thumbnail desc, i.sortOrder asc")
    List<Object[]> findImageUrlsByProductIds(@Param("productIds") Collection<Long> productIds);
}
