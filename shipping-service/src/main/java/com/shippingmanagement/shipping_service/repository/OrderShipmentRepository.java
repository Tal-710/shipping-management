package com.shippingmanagement.shipping_service.repository;

import com.shippingmanagement.shipping_service.model.OrderShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderShipmentRepository extends JpaRepository<OrderShipment, Integer> {
    Optional<OrderShipment> findByOrderId(Integer orderId);
}
