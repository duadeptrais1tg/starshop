package com.starshop.service.impl;

import com.starshop.dto.revenue.RevenueProjections;
import com.starshop.dto.revenue.RevenueReport;
import com.starshop.entity.Shop;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.OrderItemRepository;
import com.starshop.repository.OrderRepository;
import com.starshop.repository.ProductImageRepository;
import com.starshop.repository.ShopRepository;
import com.starshop.service.VendorRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorRevenueServiceImpl implements VendorRevenueService {

    private static final DateTimeFormatter LABEL_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CHART_DAY = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter CHART_MONTH = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter KEY_DAY = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter KEY_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ShopRepository shopRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductImageRepository productImageRepository;
    private final Clock clock;

    @Override
    public RevenueReport report(Long ownerId, LocalDate from, LocalDate to) {
        Shop shop = shopRepository.findByOwnerId(ownerId)
                .filter(s -> s.getStatus() == ShopStatus.APPROVED || s.getStatus() == ShopStatus.SUSPENDED)
                .orElseThrow(() -> new NotFoundException("Bạn chưa có shop đang hoạt động"));

        // Chuẩn hóa khoảng ngày: mặc định 30 ngày gần nhất, đảo nếu ngược, giới hạn độ dài
        LocalDate end = to == null ? LocalDate.now(clock) : to;
        LocalDate start = from == null ? end.minusDays(DEFAULT_DAYS - 1L) : from;
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_RANGE_DAYS) {
            start = end.minusDays(MAX_RANGE_DAYS - 1L);
        }
        LocalDateTime fromTime = start.atStartOfDay();
        LocalDateTime toTime = end.plusDays(1).atStartOfDay();
        Long shopId = shop.getId();

        RevenueProjections.Totals totals = orderRepository.revenueTotals(shopId, fromTime, toTime);

        Map<OrderStatus, Long> statusCounts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) {
            statusCounts.put(s, 0L);
        }
        for (Object[] row : orderRepository.countByStatusForShop(shopId, fromTime, toTime)) {
            statusCounts.put((OrderStatus) row[0], (Long) row[1]);
        }

        boolean monthly = ChronoUnit.DAYS.between(start, end) + 1 > DAILY_CHART_MAX_DAYS;
        List<RevenueProjections.Point> points = monthly
                ? orderRepository.revenueByMonth(shopId, fromTime, toTime)
                : orderRepository.revenueByDay(shopId, fromTime, toTime);
        Map<String, RevenueProjections.Point> byPeriod = new HashMap<>();
        points.forEach(p -> byPeriod.put(p.getPeriod(), p));

        // Lấp các ngày / tháng không có đơn bằng 0 để trục thời gian liền mạch (số liệu đã tổng hợp ở SQL)
        List<String> labels = new ArrayList<>();
        List<BigDecimal> revenue = new ArrayList<>();
        List<BigDecimal> net = new ArrayList<>();
        if (monthly) {
            for (YearMonth m = YearMonth.from(start); !m.isAfter(YearMonth.from(end)); m = m.plusMonths(1)) {
                addPoint(byPeriod.get(m.format(KEY_MONTH)), m.format(CHART_MONTH), labels, revenue, net);
            }
        } else {
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                addPoint(byPeriod.get(d.format(KEY_DAY)), d.format(CHART_DAY), labels, revenue, net);
            }
        }

        List<RevenueProjections.TopProduct> top = orderItemRepository.topProducts(
                shopId, fromTime, toTime, PageRequest.of(0, TOP_PRODUCTS));
        Map<Long, String> images = new HashMap<>();
        if (!top.isEmpty()) {
            for (Object[] row : productImageRepository.findImageUrlsByProductIds(
                    top.stream().map(RevenueProjections.TopProduct::getProductId).toList())) {
                images.putIfAbsent((Long) row[0], (String) row[1]);
            }
        }
        List<RevenueReport.TopProduct> topProducts = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            RevenueProjections.TopProduct t = top.get(i);
            topProducts.add(RevenueReport.TopProduct.builder()
                    .rank(i + 1)
                    .productId(t.getProductId())
                    .name(t.getName())
                    .slug(t.getSlug())
                    .imageUrl(images.get(t.getProductId()))
                    .quantity(t.getQuantity())
                    .revenue(t.getRevenue())
                    .build());
        }

        return RevenueReport.builder()
                .from(start)
                .to(end)
                .fromLabel(start.format(LABEL_DATE))
                .toLabel(end.format(LABEL_DATE))
                .deliveredOrders(totals.getOrderCount())
                .revenue(totals.getRevenue())
                .commission(totals.getCommission())
                .statusCounts(statusCounts)
                .monthly(monthly)
                .chartLabels(labels)
                .chartRevenue(revenue)
                .chartNet(net)
                .topProducts(topProducts)
                .build();
    }

    private static void addPoint(RevenueProjections.Point p, String label,
                                 List<String> labels, List<BigDecimal> revenue, List<BigDecimal> net) {
        labels.add(label);
        BigDecimal r = p == null ? BigDecimal.ZERO : p.getRevenue();
        BigDecimal c = p == null ? BigDecimal.ZERO : p.getCommission();
        revenue.add(r.setScale(0, RoundingMode.HALF_UP));
        net.add(r.subtract(c).setScale(0, RoundingMode.HALF_UP));
    }
}
