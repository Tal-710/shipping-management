package com.shippingmanagement.inventory_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable;

    // Instead of a direct relationship, we'll use the product ID
    // In a real-world app, we might add a @ManyToOne relationship
    // But for microservices, we'll keep them decoupled
}
