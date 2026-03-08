package com.nimbleways.springboilerplate.services.implementations;
import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.exceptions.NotFoundException;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    // ==========================================================
    // SUCCESS SCENARIOS
    // ==========================================================

    @Test
    void processOrder_ShouldProcessAllProducts_WhenOrderExists() throws NotFoundException {
        // 1. Arrange
        Long orderId = 1L;

        // Create dummy products
        Product product1 = new Product();
        product1.setId(101L);
        product1.setName("Product A");

        Product product2 = new Product();
        product2.setId(102L);
        product2.setName("Product B");

        Set<Product> products = new HashSet<>();
        products.add(product1);
        products.add(product2);

        // Create dummy order
        Order order = new Order();
        order.setId(orderId);
        order.setItems(products);

        // Mock repository behavior
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // 2. Act
        ProcessOrderResponse response = orderService.processOrder(orderId);

        // 3. Assert
        assertNotNull(response);
        assertEquals(orderId, response.id()); // Assuming ProcessOrderResponse has a getter

        // Verify that the repository was called
        verify(orderRepository, times(1)).findById(orderId);

        // Verify that handleProduct was called exactly once for EACH product in the set
        verify(productService, times(1)).handleProduct(product1);
        verify(productService, times(1)).handleProduct(product2);
        // Total invocations should match set size
        verify(productService, times(2)).handleProduct(any(Product.class));
    }

    @Test
    void processOrder_ShouldReturnResponse_WhenOrderHasNoProducts() throws NotFoundException {
        // 1. Arrange
        Long orderId = 2L;
        Order order = new Order();
        order.setId(orderId);
        order.setItems(new HashSet<>()); // Empty set

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // 2. Act
        ProcessOrderResponse response = orderService.processOrder(orderId);

        // 3. Assert
        assertEquals(orderId, response.id());

        // Verify handleProduct was NEVER called because the list is empty
        verify(productService, never()).handleProduct(any());
    }

    // ==========================================================
    // EXCEPTION SCENARIOS
    // ==========================================================

    @Test
    void processOrder_ShouldThrowException_WhenOrderNotFound() {
        // 1. Arrange
        Long invalidId = 99L;
        when(orderRepository.findById(invalidId)).thenReturn(Optional.empty());

        // 2. Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            orderService.processOrder(invalidId);
        });

        assertEquals("Order not found with id: " + invalidId, exception.getMessage());

        // Verify we never attempted to process products
        verifyNoInteractions(productService);
    }
}