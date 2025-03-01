package com.shippingmanagement.shipping_service.repository;

import com.shippingmanagement.shipping_service.model.ShipTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipRepository extends JpaRepository<ShipTracking, Integer> {
    Optional<ShipTracking> findFirstByDestinationCountryOrderByTotalOrdersAsc(String destinationCountry);
}