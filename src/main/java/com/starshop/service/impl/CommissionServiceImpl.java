package com.starshop.service.impl;

import com.starshop.dto.commission.CommissionForm;
import com.starshop.dto.commission.CommissionRateDto;
import com.starshop.dto.commission.ShopCommissionRow;
import com.starshop.entity.Shop;
import com.starshop.entity.ShopCommission;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.ShopCommissionRepository;
import com.starshop.repository.ShopRepository;
import com.starshop.repository.spec.ShopSpecifications;
import com.starshop.service.CommissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal MAX_RATE = BigDecimal.valueOf(100);

    private final ShopCommissionRepository commissionRepository;
    private final ShopRepository shopRepository;
    private final Clock clock;

    // ------------------------------------------------------------------ tra cứu

    @Override
    @Transactional(readOnly = true)
    public BigDecimal rateFor(Long shopId, LocalDate date) {
        if (shopId != null) {
            List<ShopCommission> custom = commissionRepository.findEffectiveForShop(shopId, date);
            if (!custom.isEmpty()) {
                return custom.get(0).getRate();
            }
        }
        return defaultRateOn(date);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal currentDefaultRate() {
        return defaultRateOn(today());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommissionRateDto> defaultHistory() {
        return toDtos(commissionRepository.findByShopIsNullOrderByEffectiveFromAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommissionRateDto> shopHistory(Long shopId) {
        return toDtos(commissionRepository.findByShopIdOrderByEffectiveFromAsc(shopId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopCommissionRow> shopRows(String keyword, int page) {
        Specification<Shop> spec = Specification.allOf(
                (root, query, cb) -> root.get("status").in(ShopStatus.APPROVED, ShopStatus.SUSPENDED),
                ShopSpecifications.keyword(keyword));
        Page<Shop> shops = shopRepository.findAll(spec, PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("name")));

        // Mức riêng của cả trang lấy bằng 1 query
        LocalDate today = today();
        Map<Long, BigDecimal> custom = new HashMap<>();
        List<Long> ids = shops.getContent().stream().map(Shop::getId).toList();
        if (!ids.isEmpty()) {
            for (ShopCommission c : commissionRepository.findEffectiveForShops(ids, today)) {
                custom.put(c.getShop().getId(), c.getRate());
            }
        }
        BigDecimal defaultRate = defaultRateOn(today);
        return shops.map(s -> toRow(s, custom.get(s.getId()), defaultRate));
    }

    @Override
    @Transactional(readOnly = true)
    public ShopCommissionRow shopRow(Long shopId) {
        Shop shop = findShop(shopId);
        LocalDate today = today();
        List<ShopCommission> custom = commissionRepository.findEffectiveForShop(shopId, today);
        return toRow(shop, custom.isEmpty() ? null : custom.get(0).getRate(), defaultRateOn(today));
    }

    // ------------------------------------------------------------------ thay đổi

    @Override
    @Transactional
    public void addRate(Long shopId, CommissionForm form) {
        BigDecimal rate = form.getRate();
        LocalDate from = form.getEffectiveFrom();
        LocalDate to = form.getEffectiveTo();
        if (rate == null || rate.signum() < 0 || rate.compareTo(MAX_RATE) > 0) {
            throw new BusinessException("Tỉ lệ chiết khấu phải từ 0 đến 100%.");
        }
        if (from == null || from.isBefore(today())) {
            throw new BusinessException("Chỉ được đặt mức chiết khấu áp dụng từ hôm nay trở đi"
                    + " (không sửa lại doanh thu đã qua).");
        }
        if (to != null && to.isBefore(from)) {
            throw new BusinessException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
        }
        Shop shop = shopId == null ? null : findShop(shopId);
        List<ShopCommission> existing = shopId == null
                ? commissionRepository.findByShopIsNullOrderByEffectiveFromAsc()
                : commissionRepository.findByShopIdOrderByEffectiveFromAsc(shopId);

        List<ShopCommission> toSave = new ArrayList<>();
        for (ShopCommission e : existing) {
            LocalDate eTo = e.getEffectiveTo();
            if (!e.getEffectiveFrom().isBefore(from)) {
                // Mức đã lên lịch bắt đầu từ ngày mức mới trở về sau: không cho chồng lên
                if (to == null || !e.getEffectiveFrom().isAfter(to)) {
                    if (!e.getEffectiveFrom().isAfter(today())) {
                        throw new BusinessException("Mức " + e.getRate().stripTrailingZeros().toPlainString()
                                + "% vừa bắt đầu áp dụng hôm nay. Hãy đặt mức mới từ ngày mai.");
                    }
                    throw new BusinessException("Đã có mức " + e.getRate().stripTrailingZeros().toPlainString()
                            + "% lên lịch từ " + e.getEffectiveFrom().format(DATE) + ". Hãy xóa mức đó trước.");
                }
            } else if (eTo == null || !eTo.isBefore(from)) {
                // Mức đang chạy giao với mức mới: kết thúc nó vào ngày hôm trước
                if (to != null && (eTo == null || eTo.isAfter(to))) {
                    // Mức mới có thời hạn: mức cũ tiếp tục sau khi mức mới kết thúc
                    toSave.add(ShopCommission.builder()
                            .shop(e.getShop()).rate(e.getRate())
                            .effectiveFrom(to.plusDays(1)).effectiveTo(eTo)
                            .build());
                }
                e.setEffectiveTo(from.minusDays(1));
            }
        }
        toSave.add(ShopCommission.builder().shop(shop).rate(rate).effectiveFrom(from).effectiveTo(to).build());
        commissionRepository.saveAll(toSave);
    }

    @Override
    @Transactional
    public Long deleteRate(Long id) {
        ShopCommission target = commissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mức chiết khấu #" + id));
        if (!target.getEffectiveFrom().isAfter(today())) {
            throw new BusinessException("Chỉ xóa được mức chưa bắt đầu áp dụng.");
        }
        Long shopId = target.getShop() == null ? null : target.getShop().getId();
        List<ShopCommission> sameScope = shopId == null
                ? commissionRepository.findByShopIsNullOrderByEffectiveFromAsc()
                : commissionRepository.findByShopIdOrderByEffectiveFromAsc(shopId);
        // Mức liền trước (đã bị cắt ngắn khi thêm mức này) được kéo dài lại để không có khoảng trống
        LocalDate dayBefore = target.getEffectiveFrom().minusDays(1);
        sameScope.stream()
                .filter(e -> dayBefore.equals(e.getEffectiveTo()))
                .findFirst()
                .ifPresent(prev -> prev.setEffectiveTo(target.getEffectiveTo()));
        commissionRepository.delete(target);
        return shopId;
    }

    // ------------------------------------------------------------------ helpers

    private BigDecimal defaultRateOn(LocalDate date) {
        List<ShopCommission> defaults = commissionRepository.findEffectiveDefault(date);
        return defaults.isEmpty() ? BigDecimal.ZERO : defaults.get(0).getRate();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private Shop findShop(Long shopId) {
        return shopRepository.findById(shopId).orElseThrow(() -> new NotFoundException("Không tìm thấy shop #" + shopId));
    }

    private static ShopCommissionRow toRow(Shop shop, BigDecimal customRate, BigDecimal defaultRate) {
        return ShopCommissionRow.builder()
                .shopId(shop.getId())
                .shopName(shop.getName())
                .ownerEmail(shop.getOwner().getEmail())
                .status(shop.getStatus())
                .customRate(customRate)
                .effectiveRate(customRate != null ? customRate : defaultRate)
                .build();
    }

    /** Mới nhất lên trên để dễ xem. */
    private List<CommissionRateDto> toDtos(List<ShopCommission> list) {
        LocalDate today = today();
        List<CommissionRateDto> result = new ArrayList<>();
        for (int i = list.size() - 1; i >= 0; i--) {
            ShopCommission c = list.get(i);
            CommissionRateDto.Status status;
            if (c.getEffectiveFrom().isAfter(today)) {
                status = CommissionRateDto.Status.UPCOMING;
            } else if (c.getEffectiveTo() != null && c.getEffectiveTo().isBefore(today)) {
                status = CommissionRateDto.Status.EXPIRED;
            } else {
                status = CommissionRateDto.Status.ACTIVE;
            }
            result.add(CommissionRateDto.builder()
                    .id(c.getId())
                    .rate(c.getRate())
                    .effectiveFrom(c.getEffectiveFrom().format(DATE))
                    .effectiveTo(c.getEffectiveTo() == null ? "" : c.getEffectiveTo().format(DATE))
                    .status(status)
                    .build());
        }
        return result;
    }
}
