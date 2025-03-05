package com.shippingmanagement.shipping_service.consumer;

import com.shippingmanagement.shipping_service.avro.ShipmentCreated;
import com.shippingmanagement.shipping_service.model.ShipTracking;
import com.shippingmanagement.shipping_service.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShipmentConsumer {

    private final ShipmentService shipmentService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000),
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "${app.kafka.topics.unassigned-shipping-orders}")
    public void processUnassignedShipment(ConsumerRecord<String, ShipmentCreated> record) {
        ShipmentCreated shipment = record.value();
        log.info("Processing unassigned shipment for order {}", shipment.getOrderId());

        Optional<ShipTracking> shipOptional = shipmentService.assignOrderToShip(
                shipment.getOrderId(),
                shipment.getDestinationCountry().toString()
        );

        if (shipOptional.isEmpty()) {
            log.warn("No ship available for order {} to destination {}. Will retry.",
                    shipment.getOrderId(), shipment.getDestinationCountry());

            throw new RuntimeException("No ship available for destination: " +
                    shipment.getDestinationCountry());
        }

        log.info("Ship found for previously unassigned order {}: Ship ID {}",
                shipment.getOrderId(), shipOptional.get().getShipId());
    }

    @DltHandler
    public void handleDeadLetterShipment(ConsumerRecord<String, ShipmentCreated> record) {
        try {
            ShipmentCreated shipment = record.value();
            log.error("DLT - Failed to assign ship to order {} after all retry attempts",
                    shipment.getOrderId());

            shipmentService.markOrderForManualProcessing(shipment.getOrderId());
        } catch (Exception e) {
            log.error("Error processing DLT message: {}", e.getMessage());
        }
    }
}