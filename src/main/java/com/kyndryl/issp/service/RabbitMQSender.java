package com.kyndryl.issp.service;


import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQSender {

    @Autowired
    private AmqpTemplate  rabbitTemplate;

    @Autowired
    private RabbitMQHelper rabbitMQHelper;


    public RabbitMQSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(String s) {
        rabbitTemplate.convertAndSend(rabbitMQHelper.getExchange(), rabbitMQHelper.routing_key, s);
        System.out.println("Send msg for queue = " + s);
    }

}
