package com.shippingmanagement.order_service.repository;

import com.shippingmanagement.order_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByDestinationCountry(String destinationCountry);
    List<Order> findByCustomerId(String customerId);
}
