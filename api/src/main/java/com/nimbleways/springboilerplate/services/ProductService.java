package com.nimbleways.springboilerplate.services;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public void processNormalProduct(Product product) {
        if (product.getAvailable() > 0) {
            decrementProduct(product);
        } else if (product.getLeadTime() > 0) {
            handleDelayedProduct(product, product.getLeadTime());
        }
    }

    private void handleDelayedProduct(Product product, int leadTime) {
        product.setLeadTime(leadTime);
        productRepository.save(product);
        notificationService.sendDelayNotification(leadTime, product.getName());
    }

    // Seasonal Product ----------
    public void processSeasonalProduct(Product product) {
        LocalDate now = LocalDate.now();
        boolean inSeason = !now.isBefore(product.getSeasonStartDate()) && !now.isAfter(product.getSeasonEndDate());

        if (inSeason && product.getAvailable() > 0) {
            decrementProduct(product);
        } else if (now.isBefore(product.getSeasonStartDate()) ||
                now.plusDays(product.getLeadTime()).isAfter(product.getSeasonEndDate())) {
            product.setAvailable(0);
            productRepository.save(product);
            notificationService.sendOutOfStockNotification(product.getName());
        } else {
            processNormalProduct(product);
        }
    }

    // Expirable Product ----------
    public void processExpirableProduct(Product product) {
        if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(LocalDate.now())) {
            decrementProduct(product);
        } else {
            product.setAvailable(0);
            productRepository.save(product);
            notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
        }
    }

    // Helper ----------
    private void decrementProduct(Product product) {
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
    }
}