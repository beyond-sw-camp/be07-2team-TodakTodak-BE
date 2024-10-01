package com.padaks.todaktodak.reservation.service;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaReservationProducerConfig {

//    kafka Admin 설정
    @Bean
    public KafkaAdmin kafkaAdmin(KafkaProperties kafkaProperties){
        return new KafkaAdmin(kafkaProperties.buildProducerProperties());
    }

    @Bean
    public NewTopic reservationTopic(){
        return new NewTopic("reservationImmediate", 10 , (short) 1);
    }

    @Bean
    public NewTopic reservationScheduleTopic(){
        return new NewTopic("reservationSchedule", 24, (short) 1);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(){
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(){
        return new KafkaTemplate<>(producerFactory());
    }
}
