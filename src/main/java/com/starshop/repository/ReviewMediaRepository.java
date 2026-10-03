package com.starshop.repository;

import com.starshop.entity.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Long> {

    /** Media của nhiều đánh giá trong 1 query (tránh N+1 khi hiển thị danh sách đánh giá). */
    List<ReviewMedia> findByReviewIdInOrderByIdAsc(Collection<Long> reviewIds);
}
