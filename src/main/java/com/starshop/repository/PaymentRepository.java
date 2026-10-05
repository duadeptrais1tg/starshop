package com.starshop.repository;

import com.starshop.entity.Payment;
import com.starshop.entity.enums.PaymentMethod;
import com.starshop.entity.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTxnRef(String txnRef);

    boolean existsByTxnRef(String txnRef);

    /**
     * Nạp Payment và KHÓA dòng (SELECT ... FOR UPDATE) đến hết transaction: return URL và IPN của cùng
     * giao dịch đến cùng lúc thì xử lý lần lượt, lần sau thấy đã xử lý -> bỏ qua (chống xử lý trùng).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.txnRef = :txnRef")
    Optional<Payment> findByTxnRefForUpdate(@Param("txnRef") String txnRef);

    /** Mã giao dịch online còn chờ thanh toán đã tạo trước thời điểm cho trước (khách bỏ dở -> hủy). */
    @Query("select p.txnRef from Payment p where p.method = :method and p.status = :status and p.createdAt < :before")
    List<String> findStaleTxnRefs(@Param("method") PaymentMethod method, @Param("status") PaymentStatus status,
                                  @Param("before") LocalDateTime before);
}
