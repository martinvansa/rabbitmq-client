package com.kyndryl.issp.component;


import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kyndryl.issp.config.KindOfQueue;
import com.kyndryl.issp.service.RabbitMQHelper;

@Component
public class ScheduledTaskService {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss");
    private final String myCron = "0 0 */2 * * *";

    @Autowired
    private RabbitMQHelper rabbitMQHelper;


   /*
    1.Second (0 - 59)
    2.Minute (0 - 59)
    3.Hours (0 - 23)
    4.Day-of-Month (1 - 31)
    5.Month (1 - 31)
    6.Day-of-Week (0 - 6) (Sunday=0 or 7)
    7.Year [optional]
    */

    @Scheduled(cron = myCron)
    public void checkRabbitmq() throws IOException, ParseException {
        int newValue_I;
        int newValue_D;
        int newValue_P;

        rabbitMQHelper.sendRequest(KindOfQueue.PARAM_UCI.kind);
        newValue_I = rabbitMQHelper.getQueueMessageCount();

        rabbitMQHelper.sendRequest(KindOfQueue.PARAM_UCD.kind);
        newValue_D = rabbitMQHelper.getQueueMessageCount();

        rabbitMQHelper.sendRequest(KindOfQueue.PARAM_UCP.kind);
        newValue_P = rabbitMQHelper.getQueueMessageCount();

        if ((newValue_I == rabbitMQHelper.getLastCountMessages_I() && newValue_I != 0)
            || (newValue_D == rabbitMQHelper.getLastCountMessages_D() && newValue_D != 0)
            || (newValue_P == rabbitMQHelper.getLastCountMessages_P() && newValue_P != 0)) {

            rabbitMQHelper.sendRequest(rabbitMQHelper.prepareData());
        }

        rabbitMQHelper.setLastCountMessages_I(newValue_I);
        rabbitMQHelper.setLastCountMessages_D(newValue_D);
        rabbitMQHelper.setLastCountMessages_P(newValue_P);

        System.err.println("Code is being executed... Time: " + formatter.format(LocalDateTime.now()));
    }
}
