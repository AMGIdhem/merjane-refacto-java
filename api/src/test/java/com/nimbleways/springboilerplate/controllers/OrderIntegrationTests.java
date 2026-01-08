package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.enums.ProductType;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;

import static org.junit.Assert.assertEquals;

// import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.*;

// Specify the controller class you want to test
// This indicates to spring boot to only load UsersController into the context
// Which allows a better performance and needs to do less mocks
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class OrderIntegrationTests {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NotificationService notificationService;

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private ProductRepository productRepository;

        @Test
        public void processOrderShouldReturn() throws Exception {
                List<Product> allProducts = createProducts();
                Set<Product> orderItems = new HashSet<Product>(allProducts);
                Order order = createOrder(orderItems);
                productRepository.saveAll(allProducts);
                order = orderRepository.save(order);
                mockMvc.perform(post("/orders/{orderId}/process", order.getId())
                                .contentType("application/json"))
                                .andExpect(status().isOk());
                Order resultOrder = orderRepository.findById(order.getId()).get();
                assertEquals(resultOrder.getId(), order.getId());
        }

        private static Order createOrder(Set<Product> products) {
                Order order = new Order();
                order.setItems(products);
                return order;
        }

        private static List<Product> createProducts() {
                List<Product> products = new ArrayList<>();
                products.add(new Product(null, 15, 30, ProductType.NORMAL, "USB Cable", null, null, null));
                products.add(new Product(null, 10, 0, ProductType.NORMAL, "USB Dongle", null, null, null));
                products.add(new Product(null, 15, 30, ProductType.EXPIRABLE, "Butter", LocalDate.now().plusDays(26), null,
                                null));
                products.add(new Product(null, 90, 6, ProductType.EXPIRABLE, "Milk", LocalDate.now().minusDays(2), null, null));
                products.add(new Product(null, 15, 30, ProductType.SEASONAL, "Watermelon", null, LocalDate.now().minusDays(2),
                                LocalDate.now().plusDays(58)));
                products.add(new Product(null, 15, 30, ProductType.SEASONAL, "Grapes", null, LocalDate.now().plusDays(180),
                                LocalDate.now().plusDays(240)));
                return products;
        }

    @Test
    public void processOrderWithNormalProducts_shouldDecrementAvailable() throws Exception {
        // GIVEN
        List<Product> products = List.of(
                new Product(null, 15, 2, ProductType.NORMAL, "USB Cable", null, null, null),
                new Product(null, 0, 0, ProductType.NORMAL, "USB C", null, null, null)
        );
        productRepository.saveAll(products);
        Order order = createOrder(new HashSet<>(products));
        order = orderRepository.save(order);

        // WHEN
        mockMvc.perform(post("/orders/{orderId}/process", order.getId())
                        .contentType("application/json"))
                .andExpect(status().isOk());

        Order resultOrder = orderRepository.findById(order.getId())
                .orElseThrow(() -> new AssertionError("Order not found"));

        assertEquals(order.getId(), resultOrder.getId());
    }

    // Test Expirable Products --------------------
    @Test
    public void processOrderWithExpirableProducts_shouldHandleExpiry() throws Exception {
        LocalDate today = LocalDate.now();

        Product butter = new Product(null, 15, 30, ProductType.EXPIRABLE, "Butter", today.plusDays(5), null,
                LocalDate.now().plusDays(5));
        Product milk = new Product(null, 90, 6, ProductType.EXPIRABLE, "Milk", today.plusDays(5), null,
                LocalDate.now().minusDays(1)); // expired
        productRepository.saveAll(List.of(butter, milk));

        Order order = createOrder(new HashSet<>(List.of(butter, milk)));
        order = orderRepository.save(order);

        mockMvc.perform(post("/orders/{orderId}/process", order.getId())
                        .contentType("application/json"))
                .andExpect(status().isOk());

        Product savedButter = productRepository.findById(butter.getId()).get();
        Product savedMilk = productRepository.findById(milk.getId()).get();

        assertEquals(29, (int) savedButter.getAvailable()); // decremented by 1
        assertEquals(0, (int) savedMilk.getAvailable());    // expired
        Mockito.verify(notificationService, Mockito.times(1))
                .sendExpirationNotification(milk.getName(), milk.getExpiryDate());
    }
}
