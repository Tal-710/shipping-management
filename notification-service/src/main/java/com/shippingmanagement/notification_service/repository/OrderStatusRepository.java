package com.shippingmanagement.notification_service.repository;

import com.shippingmanagement.notification_service.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface OrderStatusRepository extends JpaRepository<OrderStatus, Integer> {

    @Query("SELECT MIN(os.orderId) FROM OrderStatus os")
    Integer findSmallestOrderId();
}