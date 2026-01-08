package com.nimbleways.springboilerplate.services;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    public void processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        order.getItems().forEach(product -> {
            switch (product.getType()) {
                case NORMAL -> productService.processNormalProduct(product);
                case SEASONAL -> productService.processSeasonalProduct(product);
                case EXPIRABLE -> productService.processExpirableProduct(product);
                default -> log.warn("Unknown product type: {}", product.getType());
            }
        });
    }
}