package com.starshop.service.impl;

import com.starshop.dto.commission.CommissionForm;
import com.starshop.entity.Shop;
import com.starshop.entity.ShopCommission;
import com.starshop.exception.BusinessException;
import com.starshop.repository.ShopCommissionRepository;
import com.starshop.repository.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommissionServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);

    private final ShopCommissionRepository repo = mock(ShopCommissionRepository.class);
    private final ShopRepository shopRepository = mock(ShopRepository.class);
    private final CommissionServiceImpl service = new CommissionServiceImpl(repo, shopRepository,
            Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE));

    private Shop shop;
    private List<ShopCommission> defaults;
    private List<ShopCommission> shopRates;

    @BeforeEach
    void setUp() {
        shop = Shop.builder().name("Hoa Mai").build();
        shop.setId(7L);
        when(shopRepository.findById(7L)).thenReturn(Optional.of(shop));
        defaults = new ArrayList<>(List.of(rate(null, "10", LocalDate.of(2026, 1, 1), null)));
        shopRates = new ArrayList<>();
        when(repo.findByShopIsNullOrderByEffectiveFromAsc()).thenReturn(defaults);
        when(repo.findByShopIdOrderByEffectiveFromAsc(7L)).thenReturn(shopRates);
    }

    // ------------------------------------------------------------- tra cứu mức

    @Test
    void rateFor_prefersShopRate_thenDefault_thenZero() {
        when(repo.findEffectiveForShop(7L, TODAY)).thenReturn(List.of(rate(shop, "5", TODAY.minusDays(3), null)));
        when(repo.findEffectiveDefault(TODAY)).thenReturn(List.of(defaults.get(0)));
        assertThat(service.rateFor(7L, TODAY)).isEqualByComparingTo("5");

        when(repo.findEffectiveForShop(7L, TODAY)).thenReturn(List.of());
        assertThat(service.rateFor(7L, TODAY)).isEqualByComparingTo("10");

        when(repo.findEffectiveDefault(TODAY)).thenReturn(List.of());
        assertThat(service.rateFor(7L, TODAY)).isZero();
    }

    // -------------------------------------------------------------- thêm mức

    @Test
    void newOpenEndedRate_closesRunningRateTheDayBefore() {
        service.addRate(null, form("12", TODAY.plusDays(10), null));

        assertThat(defaults.get(0).getEffectiveTo()).isEqualTo(TODAY.plusDays(9));
        List<ShopCommission> saved = captureSaved();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getRate()).isEqualByComparingTo("12");
        assertThat(saved.get(0).getShop()).isNull();
    }

    @Test
    void temporaryRate_splitsRunningRate_soItResumesAfterwards() {
        // Mức riêng 5% cho shop trong 1 tuần, sau đó quay lại mức riêng cũ 8%
        shopRates.add(rate(shop, "8", LocalDate.of(2026, 6, 1), null));

        service.addRate(7L, form("5", TODAY.plusDays(1), TODAY.plusDays(7)));

        assertThat(shopRates.get(0).getEffectiveTo()).isEqualTo(TODAY);
        List<ShopCommission> saved = captureSaved();
        assertThat(saved).hasSize(2);
        ShopCommission resume = saved.get(0);
        assertThat(resume.getRate()).isEqualByComparingTo("8");
        assertThat(resume.getEffectiveFrom()).isEqualTo(TODAY.plusDays(8));
        assertThat(resume.getEffectiveTo()).isNull();
        assertThat(saved.get(1).getShop()).isSameAs(shop);
    }

    @Test
    void pastStartDate_orInvalidRange_isRejected() {
        assertThatThrownBy(() -> service.addRate(null, form("12", TODAY.minusDays(1), null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("từ hôm nay");
        assertThatThrownBy(() -> service.addRate(null, form("12", TODAY.plusDays(5), TODAY.plusDays(2))))
                .hasMessageContaining("Ngày kết thúc");
        assertThatThrownBy(() -> service.addRate(null, form("101", TODAY.plusDays(5), null)))
                .hasMessageContaining("0 đến 100");
        verify(repo, never()).saveAll(anyList());
    }

    @Test
    void overlappingScheduledRate_isRejected() {
        defaults.add(rate(null, "15", TODAY.plusDays(20), null));

        assertThatThrownBy(() -> service.addRate(null, form("12", TODAY.plusDays(10), null)))
                .hasMessageContaining("15%").hasMessageContaining("xóa mức đó");
        // Có ngày kết thúc trước mức đã lên lịch thì được
        service.addRate(null, form("12", TODAY.plusDays(10), TODAY.plusDays(19)));
    }

    // --------------------------------------------------------------- xóa mức

    @Test
    void deleteUpcoming_reextendsPreviousRate() {
        ShopCommission upcoming = rate(null, "12", TODAY.plusDays(10), null);
        upcoming.setId(99L);
        defaults.get(0).setEffectiveTo(TODAY.plusDays(9));
        defaults.add(upcoming);
        when(repo.findById(99L)).thenReturn(Optional.of(upcoming));

        Long shopId = service.deleteRate(99L);

        assertThat(shopId).isNull();
        assertThat(defaults.get(0).getEffectiveTo()).isNull();
        verify(repo).delete(upcoming);
    }

    @Test
    void cannotDeleteRateAlreadyStarted() {
        ShopCommission running = defaults.get(0);
        running.setId(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(running));

        assertThatThrownBy(() -> service.deleteRate(1L)).hasMessageContaining("chưa bắt đầu");
        verify(repo, never()).delete(any(ShopCommission.class));
    }

    // ---------------------------------------------------------------- helpers

    @SuppressWarnings("unchecked")
    private List<ShopCommission> captureSaved() {
        ArgumentCaptor<List<ShopCommission>> captor = ArgumentCaptor.forClass(List.class);
        verify(repo).saveAll(captor.capture());
        return captor.getValue();
    }

    private static ShopCommission rate(Shop shop, String rate, LocalDate from, LocalDate to) {
        return ShopCommission.builder().shop(shop).rate(new BigDecimal(rate)).effectiveFrom(from).effectiveTo(to).build();
    }

    private static CommissionForm form(String rate, LocalDate from, LocalDate to) {
        CommissionForm form = new CommissionForm();
        form.setRate(new BigDecimal(rate));
        form.setEffectiveFrom(from);
        form.setEffectiveTo(to);
        return form;
    }
}
