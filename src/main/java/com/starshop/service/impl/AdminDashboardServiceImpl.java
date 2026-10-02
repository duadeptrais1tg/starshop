package com.starshop.service.impl;

import com.starshop.dto.admin.AdminStats;
import com.starshop.dto.admin.ShopAdminDto;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.mapper.AdminMapper;
import com.starshop.repository.OrderRepository;
import com.starshop.repository.ShopRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final OrderRepository orderRepository;

    @Override
    public AdminStats getStats() {
        return new AdminStats(
                userRepository.count(),
                shopRepository.countByStatus(ShopStatus.APPROVED),
                shopRepository.countByStatus(ShopStatus.PENDING),
                orderRepository.count(),
                orderRepository.sumTotalByStatus(OrderStatus.DELIVERED));
    }

    @Override
    public List<ShopAdminDto> oldestPendingShops() {
        return shopRepository.findTop5ByStatusOrderByCreatedAtAsc(ShopStatus.PENDING).stream()
                .map(AdminMapper::toShopDto)
                .toList();
    }
}
