package com.shippingmanagement.notification_service.stream;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableKafkaStreams
@Slf4j
public class OrderStatusProcessor {

    @Value("${app.kafka.topics.order-status}")
    private String orderStatusTopic;

    @Value("${app.kafka.topics.latest-order-status}")
    private String latestStatusTopic;

    @Bean
    public KTable<String, String> processOrderStatuses(StreamsBuilder builder) {
        KStream<String, String> statusStream = builder.stream(orderStatusTopic);

        statusStream.foreach((key, value) -> {
            log.info("Status stream received: key={}, value={}", key, value);
        });

        KTable<String, String> latestStatus = statusStream
                .groupByKey()
                .reduce((oldValue, newValue) -> {
                    log.info("Reducing: oldValue={}, newValue={}", oldValue, newValue);
                    return newValue;
                });


        latestStatus.toStream().foreach((key, value) -> {
            log.info("Latest status update: key={}, value={}", key, value);
        });


        latestStatus.toStream().to(latestStatusTopic,
                Produced.with(Serdes.String(), Serdes.String()));

        return latestStatus;
    }
}
