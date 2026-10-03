package com.starshop.service.impl;

import com.starshop.dto.carrier.CarrierDto;
import com.starshop.dto.carrier.CarrierForm;
import com.starshop.entity.Carrier;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.CarrierRepository;
import com.starshop.repository.OrderRepository;
import com.starshop.repository.UserRepository;
import com.starshop.repository.spec.CarrierSpecifications;
import com.starshop.service.CarrierService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CarrierServiceImpl implements CarrierService {

    private final CarrierRepository carrierRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CarrierDto> search(String keyword, Boolean active, int page) {
        Page<Carrier> carriers = carrierRepository.findAll(
                Specification.allOf(CarrierSpecifications.nameContains(keyword), CarrierSpecifications.active(active)),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("name")));

        // Đếm shipper của cả trang bằng 1 query GROUP BY
        Map<Long, Long> shipperCounts = new HashMap<>();
        List<Long> ids = carriers.getContent().stream().map(Carrier::getId).toList();
        if (!ids.isEmpty()) {
            for (Object[] row : userRepository.countByCarrierIds(ids)) {
                shipperCounts.put((Long) row[0], (Long) row[1]);
            }
        }
        return carriers.map(c -> CarrierDto.builder()
                .id(c.getId())
                .name(c.getName())
                .shippingFee(c.getShippingFee())
                .active(c.isActive())
                .shipperCount(shipperCounts.getOrDefault(c.getId(), 0L))
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public CarrierForm getForm(Long id) {
        Carrier carrier = find(id);
        CarrierForm form = new CarrierForm();
        form.setName(carrier.getName());
        form.setShippingFee(carrier.getShippingFee());
        form.setActive(carrier.isActive());
        return form;
    }

    @Override
    @Transactional
    public Long create(CarrierForm form) {
        String name = form.getName().trim();
        if (carrierRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Nhà vận chuyển \"" + name + "\" đã tồn tại.");
        }
        Carrier carrier = Carrier.builder()
                .name(name)
                .shippingFee(validFee(form.getShippingFee()))
                .active(form.isActive())
                .build();
        return carrierRepository.save(carrier).getId();
    }

    @Override
    @Transactional
    public void update(Long id, CarrierForm form) {
        Carrier carrier = find(id);
        String name = form.getName().trim();
        if (carrierRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException("Nhà vận chuyển \"" + name + "\" đã tồn tại.");
        }
        carrier.setName(name);
        // Đổi phí không ảnh hưởng đơn cũ: phí ship đã được chép vào từng đơn lúc đặt
        carrier.setShippingFee(validFee(form.getShippingFee()));
        carrier.setActive(form.isActive());
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        Carrier carrier = find(id);
        carrier.setActive(!carrier.isActive());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Carrier carrier = find(id);
        if (orderRepository.existsByCarrierId(id)) {
            throw new BusinessException("\"" + carrier.getName()
                    + "\" đã có đơn hàng nên không thể xóa. Bạn có thể tắt hoạt động thay vì xóa.");
        }
        long shippers = userRepository.countByCarrierId(id);
        if (shippers > 0) {
            throw new BusinessException("\"" + carrier.getName() + "\" đang có " + shippers
                    + " người giao hàng. Hãy chuyển họ sang nhà vận chuyển khác hoặc tắt hoạt động.");
        }
        carrierRepository.delete(carrier);
    }

    private Carrier find(Long id) {
        return carrierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhà vận chuyển #" + id));
    }

    /** Kiểm tra lại ở service (không chỉ dựa vào @Valid). */
    private static BigDecimal validFee(BigDecimal fee) {
        if (fee == null || fee.signum() < 0) {
            throw new BusinessException("Phí vận chuyển phải lớn hơn hoặc bằng 0.");
        }
        return fee;
    }
}
