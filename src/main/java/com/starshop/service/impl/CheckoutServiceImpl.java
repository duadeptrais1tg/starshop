package com.starshop.service.impl;

import com.starshop.dto.account.AddressDto;
import com.starshop.dto.carrier.CarrierDto;
import com.starshop.dto.checkout.CheckoutGroup;
import com.starshop.dto.checkout.CheckoutLine;
import com.starshop.dto.checkout.CheckoutRequest;
import com.starshop.dto.checkout.CheckoutView;
import com.starshop.dto.checkout.OrderSuccessView;
import com.starshop.dto.checkout.PlacedOrderDto;
import com.starshop.dto.promotion.AutoPricing;
import com.starshop.dto.promotion.CartLine;
import com.starshop.dto.promotion.CouponDiscount;
import com.starshop.dto.promotion.CouponOption;
import com.starshop.dto.promotion.OrderAutoDiscount;
import com.starshop.entity.Address;
import com.starshop.entity.Carrier;
import com.starshop.entity.CartItem;
import com.starshop.entity.Order;
import com.starshop.entity.OrderItem;
import com.starshop.entity.OrderStatusHistory;
import com.starshop.entity.Payment;
import com.starshop.entity.Product;
import com.starshop.entity.Shop;
import com.starshop.entity.User;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.entity.enums.PaymentMethod;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.AddressRepository;
import com.starshop.repository.CarrierRepository;
import com.starshop.repository.CartItemRepository;
import com.starshop.repository.CouponRepository;
import com.starshop.repository.OrderRepository;
import com.starshop.repository.OrderStatusHistoryRepository;
import com.starshop.repository.PaymentRepository;
import com.starshop.repository.ProductImageRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.AddressService;
import com.starshop.service.CheckoutService;
import com.starshop.service.CommissionService;
import com.starshop.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    /** Phương thức đang hỗ trợ; VNPAY / MoMo làm ở chức năng thanh toán online. */
    static final Set<PaymentMethod> SUPPORTED_METHODS = Set.of(PaymentMethod.COD);
    private static final int MAX_NOTE_LENGTH = 500;
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final DateTimeFormatter TXN_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final AddressRepository addressRepository;
    private final CarrierRepository carrierRepository;
    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AddressService addressService;
    private final PromotionService promotionService;
    private final CommissionService commissionService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public CheckoutView preview(Long userId, CheckoutRequest request) {
        Draft draft = buildDraft(userId, request, true);
        return CheckoutView.builder()
                .itemIds(draft.itemIds)
                .addresses(addressService.list(userId))
                .addressId(draft.address == null ? null : draft.address.getId())
                .carriers(draft.carriers.stream().map(CheckoutServiceImpl::toCarrierDto).toList())
                .carrierId(draft.carrier == null ? null : draft.carrier.getId())
                .paymentMethods(List.of(PaymentMethod.values()))
                .paymentMethod(draft.paymentMethod)
                .groups(draft.groups.stream().map(g -> g.view).toList())
                .errors(draft.errors)
                .build();
    }

    @Override
    @Transactional
    public String placeOrder(Long userId, CheckoutRequest request) {
        // Tính lại toàn bộ trong transaction (giá, tồn kho, mã giảm giá có thể đã thay đổi từ lúc xem trước)
        Draft draft = buildDraft(userId, request, false);
        if (!draft.errors.isEmpty()) {
            throw new BusinessException(draft.errors.get(0));
        }
        User user = userRepository.getReferenceById(userId);
        LocalDateTime now = LocalDateTime.now(clock);

        // 1) Trừ tồn kho bằng UPDATE có điều kiện: không đủ hàng -> lỗi, toàn bộ transaction rollback
        for (GroupDraft group : draft.groups) {
            for (CartItem item : group.items) {
                if (productRepository.decreaseStock(item.getProduct().getId(), item.getQuantity()) == 0) {
                    throw new BusinessException("Sản phẩm \"" + item.getProduct().getName()
                            + "\" vừa hết hàng hoặc không còn đủ số lượng, vui lòng kiểm tra lại giỏ hàng.");
                }
            }
        }

        // 2) Một Payment cho cả lần đặt hàng (COD: chờ thu tiền khi giao)
        BigDecimal grandTotal = draft.groups.stream().map(g -> g.view.getTotal()).reduce(BigDecimal.ZERO, BigDecimal::add);
        Payment payment = paymentRepository.save(Payment.builder()
                .method(draft.paymentMethod)
                .amount(grandTotal)
                .txnRef(newTxnRef(now))
                .build());

        // 3) Mỗi shop một đơn, trạng thái NEW + lịch sử trạng thái
        String shippingAddress = String.join(", ", draft.address.getDetail(), draft.address.getWard(),
                draft.address.getDistrict(), draft.address.getProvince());
        List<Order> orders = new ArrayList<>();
        for (GroupDraft group : draft.groups) {
            CheckoutGroup view = group.view;
            Order order = Order.builder()
                    .code(newOrderCode(now))
                    .user(user)
                    .shop(group.shop)
                    .carrier(draft.carrier)
                    .payment(payment)
                    .coupon(group.coupon == null ? null : couponRepository.getReferenceById(group.coupon.couponId()))
                    .status(OrderStatus.NEW)
                    .paymentMethod(draft.paymentMethod)
                    .receiverName(draft.address.getReceiverName())
                    .receiverPhone(draft.address.getPhone())
                    .shippingAddress(shippingAddress)
                    .subtotal(view.getSubtotal())
                    .productDiscount(view.getProductDiscount())
                    .shippingFee(view.getShippingFee())
                    .shippingDiscount(view.getShippingDiscount())
                    .total(view.getTotal())
                    .commissionRate(commissionService.rateFor(group.shop.getId(), now.toLocalDate()))
                    .note(view.getNote())
                    .build();
            for (int i = 0; i < group.items.size(); i++) {
                CheckoutLine line = view.getLines().get(i);
                order.addItem(OrderItem.builder()
                        .product(group.items.get(i).getProduct())
                        .productName(line.getProductName())
                        .productImage(line.getImageUrl())
                        .unitPrice(line.getUnitPrice())
                        .quantity(line.getQuantity())
                        .lineTotal(line.getLineTotal())
                        .build());
            }
            orders.add(orderRepository.save(order));
            historyRepository.save(OrderStatusHistory.builder()
                    .order(order).toStatus(OrderStatus.NEW).changedBy(user).note("Đặt hàng").build());
        }

        // 4) Xóa sản phẩm đã đặt khỏi giỏ
        Long cartId = draft.groups.get(0).items.get(0).getCart().getId();
        cartItemRepository.deleteOrdered(draft.itemIds, cartId);

        // 5) Ghi lượt dùng mã (để cuối: UPDATE có điều kiện, hết lượt -> lỗi -> rollback cả lần đặt hàng)
        for (int i = 0; i < orders.size(); i++) {
            CouponDiscount coupon = draft.groups.get(i).coupon;
            if (coupon != null) {
                promotionService.recordUsage(coupon.couponId(), userId, orders.get(i).getId(), coupon.total());
            }
        }
        return payment.getTxnRef();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderSuccessView success(Long userId, String txnRef) {
        List<Order> orders = orderRepository.findByPaymentTxnRefAndUserIdOrderByIdAsc(txnRef, userId);
        if (orders.isEmpty()) {
            throw new NotFoundException("Không tìm thấy đơn hàng");
        }
        Order first = orders.get(0);
        return OrderSuccessView.builder()
                .orders(orders.stream().map(o -> PlacedOrderDto.builder()
                        .code(o.getCode())
                        .shopName(o.getShop().getName())
                        .itemCount(o.getItems().stream().mapToInt(OrderItem::getQuantity).sum())
                        .total(o.getTotal())
                        .statusLabel(o.getStatus().getLabel())
                        .build()).toList())
                .paymentMethodLabel(first.getPaymentMethod().getLabel())
                .total(orders.stream().map(Order::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add))
                .receiverName(first.getReceiverName())
                .receiverPhone(first.getReceiverPhone())
                .shippingAddress(first.getShippingAddress())
                .build();
    }

    // ------------------------------------------------------------------ tính toán

    /** Kết quả tính toán chung cho xem trước và tạo đơn. */
    private static final class Draft {
        List<Long> itemIds;
        Address address;
        List<Carrier> carriers;
        Carrier carrier;
        PaymentMethod paymentMethod;
        final List<GroupDraft> groups = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
    }

    private static final class GroupDraft {
        Shop shop;
        List<CartItem> items;
        CouponDiscount coupon;
        CheckoutGroup view;
    }

    /**
     * @param withCouponOptions true ở trang xem trước: kèm danh sách mã dùng được cho từng đơn
     */
    private Draft buildDraft(Long userId, CheckoutRequest request, boolean withCouponOptions) {
        Draft draft = new Draft();
        List<Long> requested = request.getItemIds() == null ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(request.getItemIds().stream().filter(Objects::nonNull).toList()));
        if (requested.size() > MAX_ITEMS) {
            throw new BusinessException("Mỗi lần đặt tối đa " + MAX_ITEMS + " sản phẩm.");
        }
        // Chỉ lấy dòng trong giỏ của chính user; dòng đã bị xóa / của người khác bị bỏ qua
        List<CartItem> items = requested.isEmpty() ? List.of() : cartItemRepository.findByIdInAndCartUserId(requested, userId);
        if (items.isEmpty()) {
            throw new BusinessException("Không có sản phẩm nào được chọn để đặt hàng.");
        }
        draft.itemIds = items.stream().map(CartItem::getId).sorted().toList();

        // Địa chỉ: chọn theo id (của chính user), mặc định là địa chỉ mặc định / mới nhất
        draft.address = request.getAddressId() == null ? null
                : addressRepository.findByIdAndUserId(request.getAddressId(), userId).orElse(null);
        if (draft.address == null) {
            draft.address = addressRepository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(userId)
                    .stream().findFirst().orElse(null);
        }
        if (draft.address == null) {
            draft.errors.add("Vui lòng thêm địa chỉ nhận hàng.");
        }

        draft.carriers = carrierRepository.findByActiveTrueOrderByNameAsc();
        draft.carrier = draft.carriers.stream().filter(c -> c.getId().equals(request.getCarrierId())).findFirst()
                .orElse(draft.carriers.isEmpty() ? null : draft.carriers.get(0));
        if (draft.carrier == null) {
            draft.errors.add("Hiện chưa có nhà vận chuyển nào hoạt động, vui lòng quay lại sau.");
        }
        BigDecimal shippingFee = draft.carrier == null ? BigDecimal.ZERO : draft.carrier.getShippingFee();

        draft.paymentMethod = request.getPaymentMethod() == null ? PaymentMethod.COD : request.getPaymentMethod();
        if (!SUPPORTED_METHODS.contains(draft.paymentMethod)) {
            draft.errors.add("Phương thức \"" + draft.paymentMethod.getLabel() + "\" sẽ sớm được hỗ trợ, vui lòng chọn thanh toán khi nhận hàng.");
        }

        Map<Long, String> images = new HashMap<>();
        for (Object[] row : productImageRepository.findImageUrlsByProductIds(
                items.stream().map(i -> i.getProduct().getId()).toList())) {
            images.putIfAbsent((Long) row[0], (String) row[1]);
        }
        AutoPricing pricing = promotionService.autoPricing();

        // Tách theo shop
        Map<Long, List<CartItem>> byShop = new LinkedHashMap<>();
        for (CartItem item : items) {
            byShop.computeIfAbsent(item.getProduct().getShop().getId(), k -> new ArrayList<>()).add(item);
        }
        Set<String> usedCodes = new HashSet<>();
        for (List<CartItem> shopItems : byShop.values()) {
            draft.groups.add(buildGroup(userId, shopItems, images, pricing, shippingFee, request, usedCodes,
                    withCouponOptions, draft.errors));
        }
        return draft;
    }

    private GroupDraft buildGroup(Long userId, List<CartItem> items, Map<Long, String> images, AutoPricing pricing,
                                  BigDecimal shippingFee, CheckoutRequest request, Set<String> usedCodes,
                                  boolean withCouponOptions, List<String> errors) {
        GroupDraft group = new GroupDraft();
        group.shop = items.get(0).getProduct().getShop();
        group.items = items;
        Long shopId = group.shop.getId();

        List<CheckoutLine> lines = new ArrayList<>();
        List<CartLine> cartLines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            Product p = item.getProduct();
            BigDecimal sale = pricing.salePrice(shopId, p.getCategory().getId(), p.getPrice());
            BigDecimal unit = sale != null ? sale : p.getPrice();
            BigDecimal compare = unit.compareTo(p.getPrice()) < 0 ? p.getPrice() : p.getOriginalPrice();
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(item.getQuantity()));
            String reason = CartServiceImpl.unavailableReason(p, item.getQuantity());
            if (reason != null) {
                errors.add("\"" + p.getName() + "\": " + reason + ".");
            }
            lines.add(CheckoutLine.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .productSlug(p.getSlug())
                    .imageUrl(images.get(p.getId()))
                    .unitPrice(unit)
                    .compareAtPrice(compare != null && compare.compareTo(unit) > 0 ? compare : null)
                    .quantity(item.getQuantity())
                    .lineTotal(lineTotal)
                    .unavailableReason(reason)
                    .build());
            cartLines.add(new CartLine(p.getCategory().getId(), lineTotal));
            subtotal = subtotal.add(lineTotal);
        }

        OrderAutoDiscount auto = promotionService.autoOrderDiscount(shopId, cartLines, shippingFee);

        String code = request.getCoupons() == null ? null : normalizeCode(request.getCoupons().get(shopId));
        String couponError = null;
        if (code != null) {
            if (!usedCodes.add(code)) {
                couponError = "Mỗi mã chỉ dùng cho một đơn trong một lần đặt hàng.";
            } else {
                try {
                    group.coupon = promotionService.validateCoupon(code, userId, shopId, cartLines, shippingFee);
                } catch (BusinessException e) {
                    couponError = e.getMessage();
                }
            }
            if (couponError != null) {
                errors.add("Mã \"" + code + "\" (" + group.shop.getName() + "): " + couponError);
            }
        }
        List<CouponOption> options = withCouponOptions
                ? promotionService.availableCoupons(userId, shopId, cartLines, shippingFee) : List.of();

        group.view = CheckoutGroup.builder()
                .shopId(shopId)
                .shopName(group.shop.getName())
                .shopSlug(group.shop.getSlug())
                .lines(lines)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .autoProductDiscount(auto.productDiscount())
                .autoShippingDiscount(auto.shippingDiscount())
                .autoPromotionNames(auto.promotionNames())
                .couponCode(code)
                .couponName(group.coupon == null ? null : group.coupon.promotionName())
                .couponProductDiscount(group.coupon == null ? BigDecimal.ZERO : group.coupon.productDiscount())
                .couponShippingDiscount(group.coupon == null ? BigDecimal.ZERO : group.coupon.shippingDiscount())
                .couponError(couponError)
                .couponOptions(options)
                .note(normalizeNote(request.getNotes() == null ? null : request.getNotes().get(shopId)))
                .build();
        return group;
    }

    // ------------------------------------------------------------------ helpers

    private static CarrierDto toCarrierDto(Carrier c) {
        return CarrierDto.builder().id(c.getId()).name(c.getName()).shippingFee(c.getShippingFee()).active(c.isActive()).build();
    }

    private static String normalizeCode(String code) {
        return StringUtils.hasText(code) ? code.trim().toUpperCase(Locale.ROOT) : null;
    }

    private static String normalizeNote(String note) {
        if (!StringUtils.hasText(note)) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.length() > MAX_NOTE_LENGTH ? trimmed.substring(0, MAX_NOTE_LENGTH) : trimmed;
    }

    /** Mã đơn dạng SS + ngày + 6 số ngẫu nhiên, ví dụ SS261004482913. */
    private String newOrderCode(LocalDateTime now) {
        String code;
        do {
            code = "SS" + now.format(CODE_DATE) + String.format("%06d", RANDOM.nextInt(1_000_000));
        } while (orderRepository.existsByCode(code));
        return code;
    }

    private String newTxnRef(LocalDateTime now) {
        String ref;
        do {
            ref = "PAY" + now.format(TXN_TIME) + String.format("%04d", RANDOM.nextInt(10_000));
        } while (paymentRepository.existsByTxnRef(ref));
        return ref;
    }
}
