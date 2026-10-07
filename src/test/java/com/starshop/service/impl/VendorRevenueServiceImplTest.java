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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VendorRevenueServiceImplTest {

    private static final Long OWNER_ID = 3L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);

    private final ShopRepository shopRepository = mock(ShopRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
    private final ProductImageRepository imageRepository = mock(ProductImageRepository.class);
    private final VendorRevenueServiceImpl service = new VendorRevenueServiceImpl(shopRepository, orderRepository,
            orderItemRepository, imageRepository, Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE));

    private Shop shop;

    @BeforeEach
    void setUp() {
        shop = Shop.builder().id(1L).status(ShopStatus.APPROVED).build();
        when(shopRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(shop));
        RevenueProjections.Totals totals = totals(3, "1500000", "150000.40");
        when(orderRepository.revenueTotals(eq(1L), any(), any())).thenReturn(totals);
    }

    @Test
    void defaultRange_last30Days_dailyChart_fillsMissingDaysWithZero() {
        LocalDateTime from = LocalDate.of(2026, 9, 8).atStartOfDay();
        LocalDateTime to = LocalDate.of(2026, 10, 8).atStartOfDay();   // hết ngày 07/10
        List<RevenueProjections.Point> points = List.of(
                point("2026-09-10", "500000", "50000"), point("2026-10-07", "1000000", "100000"));
        when(orderRepository.revenueByDay(1L, from, to)).thenReturn(points);

        RevenueReport report = service.report(OWNER_ID, null, null);

        assertThat(report.getFrom()).isEqualTo(LocalDate.of(2026, 9, 8));
        assertThat(report.getTo()).isEqualTo(TODAY);
        assertThat(report.isMonthly()).isFalse();
        assertThat(report.getChartLabels()).hasSize(30).startsWith("08/09").endsWith("07/10");
        assertThat(report.getChartRevenue().get(2)).isEqualByComparingTo("500000");   // 10/09
        assertThat(report.getChartNet().get(2)).isEqualByComparingTo("450000");
        assertThat(report.getChartRevenue().get(0)).isZero();
        assertThat(report.getChartRevenue().get(29)).isEqualByComparingTo("1000000");
        verify(orderRepository, never()).revenueByMonth(any(), any(), any());
    }

    @Test
    void totals_netEqualsRevenueMinusCommission() {
        RevenueReport report = service.report(OWNER_ID, TODAY, TODAY);

        assertThat(report.getDeliveredOrders()).isEqualTo(3);
        assertThat(report.getRevenue()).isEqualByComparingTo("1500000");
        assertThat(report.getCommissionRounded()).isEqualByComparingTo("150000");
        assertThat(report.getNet()).isEqualByComparingTo("1350000");
    }

    @Test
    void longRange_usesMonthlyChart_andSwapsReversedDates() {
        List<RevenueProjections.Point> points = List.of(point("2026-08", "2000000", "200000"));
        when(orderRepository.revenueByMonth(eq(1L), any(), any())).thenReturn(points);

        RevenueReport report = service.report(OWNER_ID, TODAY, LocalDate.of(2026, 6, 15));   // đảo ngược

        assertThat(report.getFrom()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(report.getTo()).isEqualTo(TODAY);
        assertThat(report.isMonthly()).isTrue();
        assertThat(report.getChartLabels()).containsExactly("06/2026", "07/2026", "08/2026", "09/2026", "10/2026");
        assertThat(report.getChartRevenue().get(2)).isEqualByComparingTo("2000000");
        verify(orderRepository, never()).revenueByDay(any(), any(), any());
    }

    @Test
    void veryLongRange_isClamped() {
        RevenueReport report = service.report(OWNER_ID, LocalDate.of(2020, 1, 1), TODAY);
        assertThat(report.getFrom()).isEqualTo(TODAY.minusDays(730));
    }

    @Test
    void statusCounts_includeAllStatuses_andTopProductsAreRanked() {
        when(orderRepository.countByStatusForShop(eq(1L), any(), any())).thenReturn(List.of(
                new Object[]{OrderStatus.DELIVERED, 3L}, new Object[]{OrderStatus.CANCELLED, 1L}));
        RevenueProjections.TopProduct rose = top(10L, "Hoa hồng", 7, "2100000");
        RevenueProjections.TopProduct lily = top(11L, "Hoa ly", 2, "900000");
        when(orderItemRepository.topProducts(eq(1L), any(), any(), any())).thenReturn(List.of(rose, lily));
        when(imageRepository.findImageUrlsByProductIds(anyList())).thenReturn(List.<Object[]>of(new Object[]{10L, "rose.png"}));

        RevenueReport report = service.report(OWNER_ID, TODAY, TODAY);

        assertThat(report.getStatusCounts()).hasSize(OrderStatus.values().length);
        assertThat(report.getStatusCounts().get(OrderStatus.DELIVERED)).isEqualTo(3);
        assertThat(report.getStatusCounts().get(OrderStatus.NEW)).isZero();
        assertThat(report.getTotalOrders()).isEqualTo(4);
        assertThat(report.getTopProducts()).extracting(RevenueReport.TopProduct::getRank).containsExactly(1, 2);
        assertThat(report.getTopProducts().get(0).getImageUrl()).isEqualTo("rose.png");
        assertThat(report.getTopProducts().get(1).getImageUrl()).isNull();
    }

    @Test
    void vendorWithoutActiveShop_notFound() {
        shop.setStatus(ShopStatus.PENDING);
        assertThatThrownBy(() -> service.report(OWNER_ID, null, null)).isInstanceOf(NotFoundException.class);
    }

    private static RevenueProjections.Totals totals(long count, String revenue, String commission) {
        RevenueProjections.Totals t = mock(RevenueProjections.Totals.class);
        when(t.getOrderCount()).thenReturn(count);
        when(t.getRevenue()).thenReturn(new BigDecimal(revenue));
        when(t.getCommission()).thenReturn(new BigDecimal(commission));
        return t;
    }

    private static RevenueProjections.Point point(String period, String revenue, String commission) {
        RevenueProjections.Point p = mock(RevenueProjections.Point.class);
        when(p.getPeriod()).thenReturn(period);
        when(p.getRevenue()).thenReturn(new BigDecimal(revenue));
        when(p.getCommission()).thenReturn(new BigDecimal(commission));
        return p;
    }

    private static RevenueProjections.TopProduct top(Long id, String name, long qty, String revenue) {
        RevenueProjections.TopProduct t = mock(RevenueProjections.TopProduct.class);
        when(t.getProductId()).thenReturn(id);
        when(t.getName()).thenReturn(name);
        when(t.getSlug()).thenReturn("slug-" + id);
        when(t.getQuantity()).thenReturn(qty);
        when(t.getRevenue()).thenReturn(new BigDecimal(revenue));
        return t;
    }
}
