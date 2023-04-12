package com.kyndryl.issp.service;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RabbitMQListner implements MessageListener {


    public void onMessage(Message message) {
        String body = new String(message.getBody());
        log.info("Body {}", body);

        System.out.println("Consuming Message - " + new String(message.getBody()));
       if (body != null) {
          // System.out.println("Count queues - " + i);
       }
    }



}
