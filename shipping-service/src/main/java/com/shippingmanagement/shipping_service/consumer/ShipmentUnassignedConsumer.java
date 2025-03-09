package com.shippingmanagement.shipping_service.consumer;

import com.shippingmanagement.shipping_service.avro.ShipmentCreated;
import com.shippingmanagement.shipping_service.model.ShipTracking;
import com.shippingmanagement.shipping_service.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShipmentUnassignedConsumer {

    private final ShipmentService shipmentService;
    private final KafkaTemplate<String, ShipmentCreated> kafkaTemplate;

    @Value("${app.kafka.topics.shipment-created}")
    private String shipmentCreatedTopic;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 5000),
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

        ShipTracking ship = shipOptional.get();
        ShipmentCreated updatedShipment = ShipmentCreated.newBuilder(shipment)
                .setShipId(ship.getShipId())
                .setDepartureDate(ship.getDepartureDate().atZone(ZoneId.systemDefault()).toInstant())
                .build();

        kafkaTemplate.send(shipmentCreatedTopic,
                String.valueOf(shipment.getOrderId()),
                updatedShipment);

        log.info("Ship found for previously unassigned order {}: Ship ID {}. Sent to shipment-created topic.",
                shipment.getOrderId(), ship.getShipId());
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