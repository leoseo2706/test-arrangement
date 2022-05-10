package com.fiats.arrangement.config;

import com.fiats.arrangement.common.ArrangementEventCode;
import com.fiats.tmgcoreutils.common.EventCode;
import com.fiats.tmgkafka.utils.KafkaUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Configuration
public class KafkaClientConfig {

    @Value("${custom.kafka.topic.prefix}")
    private String KAFKA_TOPIC_PREFIX;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${custom.kafka.topic.partitions}")
    private int partitions;

    @Value("${custom.kafka.topic.replica}")
    private int replicas;

    @Autowired
    KafkaAdmin kafkaAdmin;

    @Bean
    public void initTopics() {
        AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        List<NewTopic> topics = new ArrayList<>();
        EnumSet.allOf(ArrangementEventCode.class).forEach(e -> topics.add(
                TopicBuilder.name(e.getTopicName(KAFKA_TOPIC_PREFIX, applicationName))
//                        .compact()
                        .partitions(partitions)
                        .replicas(replicas)
                        .build()));
        KafkaUtils.addTopicsIfNeeded(admin, topics);
    }
}
