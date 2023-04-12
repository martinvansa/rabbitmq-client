package com.kyndryl.issp;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SpringBootRabbitMQ  {


    public static void main(String[] args) {
        SpringApplication.run(SpringBootRabbitMQ.class, args);
    }

}
