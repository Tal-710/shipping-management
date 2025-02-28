package com.shippingmanagement.shipping_service.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shippingmanagement.shipping_service.avro.ShipmentCreated;
import com.shippingmanagement.shipping_service.dto.OrderSubmittedEvent;
import com.shippingmanagement.shipping_service.model.ShipTracking;
import com.shippingmanagement.shipping_service.repository.ShipRepository;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class OrderShipmentStream {

    private final ShipRepository shipRepository;
    private final ObjectMapper objectMapper;
    private final SpecificAvroSerde<?> shipmentCreatedSerde;

    @Value("${app.kafka.topics.order-submitted}")
    private String orderSubmittedTopic;

    @Value("${app.kafka.topics.shipment-created}")
    private String shipmentCreatedTopic;

    @SuppressWarnings("unchecked")
    @Bean
    public KStream<String, ShipmentCreated> processOrderStream(StreamsBuilder streamsBuilder) {
        // Create JSON serde for OrderSubmittedEvent
        final Serde<OrderSubmittedEvent> orderSerde = new JsonSerde<>(OrderSubmittedEvent.class, objectMapper);

        // Create stream from order-submitted topic
        KStream<String, String> orderJsonStream = streamsBuilder.stream(
                orderSubmittedTopic,
                Consumed.with(Serdes.String(), Serdes.String())
        );

        // Parse JSON to OrderSubmittedEvent
        KStream<String, OrderSubmittedEvent> orderStream = orderJsonStream
                .mapValues(orderJson -> {
                    try {
                        return objectMapper.readValue(orderJson, OrderSubmittedEvent.class);
                    } catch (Exception e) {
                        log.error("Error deserializing order JSON: {}", orderJson, e);
                        return null;
                    }
                })
                .filter((key, value) -> value != null);

        // Transform order events to shipment events
        KStream<String, ShipmentCreated> shipmentStream = orderStream.mapValues(orderEvent -> {
            log.info("Processing order {} for destination {}", orderEvent.getOrderId(), orderEvent.getDestinationCountry());

            // Find a ship going to the destination
            ShipTracking ship = findOrCreateShipForDestination(orderEvent.getDestinationCountry());

            // Update ship's total orders
            ship.setTotalOrders(ship.getTotalOrders() + 1);
            shipRepository.save(ship);

            // Create the shipment event in Avro format
            String shipmentId = UUID.randomUUID().toString();

            // Create Instant objects for timestamps
            Instant createdAtInstant = orderEvent.getCreatedAt() != null
                    ? orderEvent.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()
                    : Instant.now();

            Instant departureDateInstant = ship.getDepartureDate() != null
                    ? ship.getDepartureDate().atZone(ZoneId.systemDefault()).toInstant()
                    : Instant.now().plus(7, ChronoUnit.DAYS);

            ShipmentCreated shipmentCreated = ShipmentCreated.newBuilder()
                    .setShipmentId(shipmentId)
                    .setOrderId(orderEvent.getOrderId())
                    .setCustomerId(orderEvent.getCustomerId())
                    .setDestinationCountry(orderEvent.getDestinationCountry())
                    .setShipId(ship.getShipId())
                    .setDepartureDate(departureDateInstant)
                    .setCreatedAt(createdAtInstant)
                    .build();

            return shipmentCreated;
        });

        // Write to shipment-created topic in Avro format
        shipmentStream.to(
                shipmentCreatedTopic,
                Produced.with(Serdes.String(), (Serde<ShipmentCreated>) shipmentCreatedSerde)
        );

        return shipmentStream;
    }

    private ShipTracking findOrCreateShipForDestination(String destinationCountry) {
        Optional<ShipTracking> existingShip = shipRepository
                .findFirstByDestinationCountryOrderByTotalOrdersAsc(destinationCountry);

        if (existingShip.isPresent()) {
            log.info("Found existing ship {} for destination {}",
                    existingShip.get().getShipId(), destinationCountry);
            return existingShip.get();
        } else {
            // Create a new ship for this destination
            log.info("Creating new ship for destination {}", destinationCountry);
            ShipTracking newShip = ShipTracking.builder()
                    .destinationCountry(destinationCountry)
                    .totalOrders(0)
                    .departureDate(LocalDateTime.now().plus(7, ChronoUnit.DAYS))
                    .build();

            return shipRepository.save(newShip);
        }
    }
}