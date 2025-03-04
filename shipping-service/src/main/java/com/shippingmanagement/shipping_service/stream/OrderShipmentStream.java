package com.shippingmanagement.shipping_service.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shippingmanagement.shipping_service.avro.ShipmentCreated;
import com.shippingmanagement.shipping_service.dto.OrderSubmittedEvent;
import com.shippingmanagement.shipping_service.model.ShipTracking;
import com.shippingmanagement.shipping_service.service.ShipmentService;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.kafka.streams.kstream.BranchedKStream;


import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class OrderShipmentStream {

    private final ShipmentService shipmentService;
    private final ObjectMapper objectMapper;
    private final SpecificAvroSerde<ShipmentCreated> shipmentCreatedSerde;

    @Value("${app.kafka.topics.order-submitted}")
    private String orderSubmittedTopic;

    @Value("${app.kafka.topics.shipment-created}")
    private String shipmentCreatedTopic;

    @Value("${app.kafka.topics.unassigned-shipping-orders}")
    private String unassignedShippingOrdersTopic;

    @SuppressWarnings("unchecked")
    @Bean
    public KStream<String, ShipmentCreated> processOrderStream(StreamsBuilder streamsBuilder) {

        KStream<String, String> orderJsonStream = streamsBuilder.stream(
                orderSubmittedTopic,
                Consumed.with(Serdes.String(), Serdes.String())
        );

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

        KStream<String, ShipmentCreated> shipmentStream = orderStream.mapValues(orderEvent -> {
            log.info("Processing order {} for destination {}", orderEvent.getOrderId(), orderEvent.getDestinationCountry());

            Optional<ShipTracking> shipOptional = shipmentService.assignOrderToShip(
                    orderEvent.getOrderId(),
                    orderEvent.getDestinationCountry()
            );

            if (shipOptional.isEmpty()) {
                log.warn("No ship available for destination {}. Order {} cannot be processed.",
                        orderEvent.getDestinationCountry(), orderEvent.getOrderId());

                return createNoShipFoundEvent(orderEvent);
            }

            ShipTracking ship = shipOptional.get();

            String shipmentId = UUID.randomUUID().toString();

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

        KStream<String, ShipmentCreated>[] branches = shipmentStream.branch(
                (key, shipment) -> shipment.getShipId() > 0,
                (key, shipment) -> shipment.getShipId() == 0
        );

        branches[0].to(
                shipmentCreatedTopic,
                Produced.with(Serdes.String(), shipmentCreatedSerde)
        );

        branches[1].to(
                unassignedShippingOrdersTopic,
                Produced.with(Serdes.String(), shipmentCreatedSerde)
        );

        return shipmentStream;
    }

    private ShipmentCreated createNoShipFoundEvent(OrderSubmittedEvent orderEvent) {
        return ShipmentCreated.newBuilder()
                .setShipmentId(UUID.randomUUID().toString())
                .setOrderId(orderEvent.getOrderId())
                .setCustomerId(orderEvent.getCustomerId())
                .setDestinationCountry(orderEvent.getDestinationCountry())
                .setShipId(0)
                .setDepartureDate(Instant.now())
                .setCreatedAt(Instant.now())
                .build();
    }
}