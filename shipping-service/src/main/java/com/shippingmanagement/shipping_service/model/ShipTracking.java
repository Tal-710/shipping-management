package com.shippingmanagement.shipping_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ship_tracking")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ship_id")
    private Integer shipId;

    @Column(name = "destination_country", length = 100)
    private String destinationCountry;

    @Column(name = "total_orders")
    private Integer totalOrders;

    @Column(name = "departure_date")
    private LocalDateTime departureDate;

    @OneToMany(mappedBy = "shipTracking", cascade = CascadeType.ALL)
    private List<OrderShipment> orderShipments = new ArrayList<>();

    public void addOrderShipment(OrderShipment orderShipment) {
        orderShipments.add(orderShipment);
        orderShipment.setShipTracking(this);
    }
}
