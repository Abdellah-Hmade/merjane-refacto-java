package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.exceptions.NotFoundException;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.Set;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;
    public ProcessOrderResponse processOrder(Long orderId) throws NotFoundException {
        Order order = orderRepository.findById(orderId).orElseThrow(()-> new NotFoundException("Order not found with id: " + orderId));
        Set<Product> products = order.getItems();
        for (Product p : products) {
            productService.handleProduct(p);
        }

        return new ProcessOrderResponse(order.getId());
    }

}
