package com.padaks.todaktodak.common.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class ChatKafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootStrapServer;

    @Value("${spring.kafka.chat.consumer.group-id}")
    private String groupId; // chat-group

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffset; // earliest

    @Bean(name = "chatKafkaConsumerFactory")
    public ConsumerFactory<String, Object> chatKafkaConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer); // localhost:9092
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId); // chat-group
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffset); // earliest
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean(name = "chatKafkaListenerContainerFactory") // Kafka 리스너 생성 팩토리
    public ConcurrentKafkaListenerContainerFactory<String, Object> chatKafkaListenerContainerFactory() {
        // 다중 스레드에서 kafka 메시지 소비할 수 있도록 설정하는 Kafka 리스너 팩토리
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(chatKafkaConsumerFactory()); // chatKafkaConsumerFactory로 부터 Consumer설정 받아와서 사용
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD); // 메시지 소비 후 자동으로 확인(acknowledge)할지 여부
        // MANUAL : 수동확인설정. 메시지 직접 처리완료로 표시 , RECORD : 메시지마다 자동으로 확인

        System.out.println("chatKafkaListenerContainerFactory Bean has been created! : 채팅 카프카 리스너 컨테이너");

        return factory; // 이 팩토리를 사용하여 Kafka 리스너가 메시지를 소비
    }

}
