package com.fiats.arrangement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EntityScan(basePackages = "com.fiats.arrangement.jpa.entity")
@EnableJpaRepositories(basePackages = "com.fiats.arrangement.jpa.repo")
@EnableRedisRepositories(basePackages = "com.fiats.arrangement.redis.repo")
@ComponentScan(basePackages = {"com.fiats", "com.neo"})
@MapperScan(basePackages = "com.fiats.arrangement.mapper")
public class ArrangementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArrangementApplication.class, args);
    }

}
