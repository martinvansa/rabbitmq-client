package com.kyndryl.issp.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kyndryl.issp.service.RabbitMQHelper;
import com.kyndryl.issp.service.RabbitMQSender;

@RestController
@RequestMapping(value = "/rabbitmq-client/")
public class RabbitMQWebController {

    @Autowired
    RabbitMQSender rabbitMQSender;

    @Autowired
    private RabbitMQHelper rabbitMQHelper;


    @GetMapping(value = "/queue")
    public String producer(@RequestParam("q") String q) throws Exception {
        rabbitMQHelper.selectQueue(q);
        //rabbitMQSender.send(q);
        // emailServiceRMQ.SendMail(rabbitMQHelper.getQueuePropAsMessage(), rabbitMQHelper.getQueueMessageCount());
        return rabbitMQHelper.sendRequest(q);
    }


}
