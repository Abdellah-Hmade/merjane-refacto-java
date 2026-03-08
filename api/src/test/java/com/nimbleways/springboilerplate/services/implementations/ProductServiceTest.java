package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
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
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@UnitTest
public class ProductServiceTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private ProductRepository productRepository;
    @InjectMocks 
    private ProductService productService;

    @Test
    public void testNotifyDelay() {
        // GIVEN
        Product product =new Product(null, 15, 0, "NORMAL", "RJ45 Cable", null, null, null);

        Mockito.when(productRepository.save(product)).thenReturn(product);

        // WHEN
        productService.notifyDelay(product.getLeadTime(), product);

        // THEN
        assertEquals(0, product.getAvailable());
        assertEquals(15, product.getLeadTime());
        verify(productRepository, times(1)).save(product);
        verify(notificationService, times(1)).sendDelayNotification(product.getLeadTime(), product.getName());
    }

        @Test
        void handleNormalProduct_ShouldDecrementStock_WhenAvailable() {
            // Arrange
            Product product = new Product();
            product.setType("NORMAL");
            product.setAvailable(10);
            product.setName("Normal Item");

            // Act
            productService.handleProduct(product);

            // Assert
            assertEquals(9, product.getAvailable());
            verify(productRepository, times(1)).save(product);
            verifyNoInteractions(notificationService);
        }

        @Test
        void handleNormalProduct_ShouldNotifyOOS_WhenStockIsZero() {
            // Arrange
            Product product = new Product();
            product.setType("NORMAL");
            product.setAvailable(0);
            product.setName("Normal Item");

            // Act
            productService.handleProduct(product);

            // Assert
            assertEquals(0, product.getAvailable());
            verify(productRepository, never()).save(any()); // Should not save if just notifying OOS in this logic
            verify(notificationService, times(1)).sendOutOfStockNotification("Normal Item");
        }

        @Test
        void handleExpiredProduct_ShouldDecrementStock_WhenAvailableAndNotExpired() {
            // Arrange
            Product product = new Product();
            product.setType("EXPIRABLE");
            product.setAvailable(10);
            product.setExpiryDate(LocalDate.now().plusDays(5)); // Future date
            product.setName("Milk");

            // Act
            productService.handleProduct(product);

            // Assert
            assertEquals(9, product.getAvailable());
            verify(productRepository, times(1)).save(product);
            verifyNoInteractions(notificationService);
        }

        @Test
        void handleExpiredProduct_ShouldSetStockZeroAndNotify_WhenExpired() {
            // Arrange
            Product product = new Product();
            product.setType("EXPIRABLE");
            product.setAvailable(10);
            LocalDate yesterday = LocalDate.now().minusDays(1);
            product.setExpiryDate(yesterday);
            product.setName("Old Milk");

            // Act
            productService.handleProduct(product);

            // Assert
            assertEquals(0, product.getAvailable());
            verify(productRepository, times(1)).save(product);
            verify(notificationService, times(1)).sendExpirationNotification("Old Milk", yesterday);
        }

        @Test
        void handleExpiredProduct_ShouldSetStockZeroAndNotify_WhenStockIsZeroWait() {

            Product product = new Product();
            product.setType("EXPIRABLE");
            product.setAvailable(0);
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            product.setExpiryDate(tomorrow);
            product.setName("Empty Milk");

            // Act
            productService.handleProduct(product);

            // Assert
            assertEquals(0, product.getAvailable());
            verify(productRepository, times(1)).save(product);
            verify(notificationService, times(1)).sendExpirationNotification("Empty Milk", tomorrow);
        }

        // ==========================================================
        // SEASONAL PRODUCT TESTS
        // ==========================================================

        @Test
        void handleSeasonalProduct_ShouldNotifyDelay_WhenInSeason() {
            Product product = new Product();
            product.setType("SEASONAL");
            product.setAvailable(10);
            product.setName("Summer Hat");
            product.setLeadTime(5);
            product.setSeasonStartDate(LocalDate.now().minusDays(10));
            product.setSeasonEndDate(LocalDate.now().plusDays(20)); // 5 days lead time < 20 days remaining

            productService.handleProduct(product);


            assertEquals(5, product.getLeadTime()); // notifyDelay sets leadTime
            verify(productRepository, times(1)).save(product);
            verify(notificationService, times(1)).sendDelayNotification(5, "Summer Hat");
        }

        @Test
        void handleSeasonalProduct_ShouldNotifyOOS_WhenTooLateInSeason() {

            Product product = new Product();
            product.setType("SEASONAL");
            product.setName("Late Item");
            product.setLeadTime(10);
            product.setSeasonStartDate(LocalDate.now().minusDays(10));
            product.setSeasonEndDate(LocalDate.now().plusDays(5)); // 10 days lead is after 5 days remaining

            productService.handleProduct(product);

            assertEquals(0, product.getAvailable());
            verify(notificationService, times(1)).sendOutOfStockNotification("Late Item");
            verify(productRepository, times(1)).save(product);
        }

        @Test
        void handleSeasonalProduct_ShouldNotifyOOS_WhenTooEarlyInSeason() {

            Product product = new Product();
            product.setType("SEASONAL");
            product.setName("Early Item");
            product.setLeadTime(5);
            product.setSeasonStartDate(LocalDate.now().plusDays(5)); // Season starts in 5 days
            product.setSeasonEndDate(LocalDate.now().plusDays(20));

            productService.handleProduct(product);

            verify(notificationService, times(1)).sendOutOfStockNotification("Early Item");
            verify(productRepository, times(1)).save(product);
        }

}