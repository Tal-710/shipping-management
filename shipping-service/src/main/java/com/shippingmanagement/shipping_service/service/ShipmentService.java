package com.shippingmanagement.shipping_service.service;

import com.shippingmanagement.shipping_service.model.OrderShipment;
import com.shippingmanagement.shipping_service.model.ShipTracking;
import com.shippingmanagement.shipping_service.repository.OrderShipmentRepository;
import com.shippingmanagement.shipping_service.repository.ShipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentService {

    private final ShipRepository shipRepository;
    private final OrderShipmentRepository orderShipmentRepository;

    public Optional<ShipTracking> findShipForDestination(String destinationCountry) {
        Optional<ShipTracking> existingShip = shipRepository
                .findFirstByDestinationCountryOrderByTotalOrdersAsc(destinationCountry);

        if (existingShip.isPresent()) {
            log.info("Found existing ship {} for destination {}",
                    existingShip.get().getShipId(), destinationCountry);
        } else {
            log.warn("No ship available for destination {}", destinationCountry);
        }

        return existingShip;
    }

    @Transactional
    public ShipTracking updateShipOrderCount(ShipTracking ship) {
        ship.setTotalOrders(ship.getTotalOrders() + 1);
        ShipTracking updatedShip = shipRepository.save(ship);
        log.info("Updated ship {} total orders to {}", ship.getShipId(), ship.getTotalOrders());
        return updatedShip;
    }


    @Transactional
    public void createOrderShipment(Integer orderId, ShipTracking ship) {
        Optional<OrderShipment> existingAssignment = orderShipmentRepository.findByOrderId(orderId);
        if (existingAssignment.isPresent()) {
            log.info("Order {} is already assigned to ship {}",
                    orderId, existingAssignment.get().getShipTracking().getShipId());
            return;
        }


        OrderShipment orderShipment = OrderShipment.builder()
                .orderId(orderId)
                .shipTracking(ship)
                .assignedDate(LocalDateTime.now())
                .build();

        orderShipmentRepository.save(orderShipment);
        log.info("Created order_shipment record for order {} and ship {}", orderId, ship.getShipId());
    }


    @Transactional
    public Optional<ShipTracking> assignOrderToShip(Integer orderId, String destinationCountry) {
        Optional<ShipTracking> shipOptional = findShipForDestination(destinationCountry);

        if (shipOptional.isEmpty()) {
            return Optional.empty();
        }

        ShipTracking ship = shipOptional.get();

        createOrderShipment(orderId, ship);

        return Optional.of(updateShipOrderCount(ship));
    }
}