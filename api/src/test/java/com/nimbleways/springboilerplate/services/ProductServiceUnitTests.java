package com.nimbleways.springboilerplate.services;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.enums.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@UnitTest
public class ProductServiceUnitTests {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks 
    private ProductService productService;

    // Normal Product --------------------
    @Test
    public void testProcessNormalProductDelayed() {
        // GIVEN
        Product product = new Product(null, 15, 0, ProductType.NORMAL, "RJ45 Cable", null, null, null);

        Mockito.when(productRepository.save(product)).thenReturn(product);

        // WHEN
        productService.processNormalProduct(product);

        // THEN
        assertEquals(0, product.getAvailable());
        assertEquals(15, product.getLeadTime());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verify(notificationService, Mockito.times(1))
                .sendDelayNotification(product.getLeadTime(), product.getName());
    }

    // Seasonal Product --------------------
    @Test
    public void testProcessSeasonalProductInSeason() {
        LocalDate today = LocalDate.now();
        Product product = new Product(
                null,
                0,  // leadTime
                5,  // available
                ProductType.SEASONAL,
                "Jacket",
                today.minusDays(1),
                today.plusDays(5),
                null
        );

        productService.processSeasonalProduct(product);

        assertEquals(4, product.getAvailable());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    public void testProcessSeasonalProductOutOfSeasonBeforeStart() {
        LocalDate today = LocalDate.now();
        Product product = new Product(null, 2, 5, ProductType.SEASONAL, "Jacket",
                today.plusDays(5), today.plusDays(10), null);

        Mockito.when(productRepository.save(product)).thenReturn(product);

        productService.processSeasonalProduct(product);

        assertEquals(0, product.getAvailable());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verify(notificationService, Mockito.times(1))
                .sendOutOfStockNotification(product.getName());
    }

    @Test
    public void testProcessSeasonalProductOutOfSeasonLeadTimeExceeds() {
        LocalDate today = LocalDate.now();
        Product product = new Product(null, 10, 5, ProductType.SEASONAL, "Jacket",
                today.minusDays(1), today.plusDays(5), null);

        Mockito.when(productRepository.save(product)).thenReturn(product);

        productService.processSeasonalProduct(product);

        assertEquals(0, product.getAvailable());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verify(notificationService, Mockito.times(1))
                .sendDelayNotification(product.getLeadTime(), product.getName());
    }

    // Expirable Product --------------------
    @Test
    public void testProcessExpirableProductValid() {
        Product product = new Product(null, 0, 5, ProductType.EXPIRABLE, "Milk",
                null, null, LocalDate.now().plusDays(2));

        Mockito.when(productRepository.save(product)).thenReturn(product);

        productService.processExpirableProduct(product);

        assertEquals(4, product.getAvailable());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    public void testProcessExpirableProductExpired() {
        Product product = new Product(null, 0, 5, ProductType.EXPIRABLE, "Milk",
                null, null, LocalDate.now().minusDays(1));

        Mockito.when(productRepository.save(product)).thenReturn(product);

        productService.processExpirableProduct(product);

        assertEquals(0, product.getAvailable());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verify(notificationService, Mockito.times(1))
                .sendExpirationNotification(product.getName(), product.getExpiryDate());
    }
}