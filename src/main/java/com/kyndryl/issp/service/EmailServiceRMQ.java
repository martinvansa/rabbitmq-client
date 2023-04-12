package com.kyndryl.issp.service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.stereotype.Service;


@Service
public class EmailServiceRMQ {

    private final String TO_EMAIL = "marvanel55@gmail.com";
    private final String FROM_EMAIL = "vansamartin@gmail.com";
    private final String HOST = "smtp.gmail.com";
    private int countMessagesMax = 8;

    private int sizeLastQueue;

    public void SendMail(String textMessage, int countMessages) {
        Properties properties = System.getProperties();

        // Setup mail server
        properties.put("mail.smtp.host", HOST);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        // Get the Session object.// and pass username and password
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            String psw = "zujgzpbobekizzoy";

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, psw);
            }
        });

        // Used to debug SMTP issues
        //session.setDebug(true);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));

            message.addRecipient(Message.RecipientType.TO, new InternetAddress(TO_EMAIL));

            message.setSubject("Queue message count - env. ");

            message.setText(textMessage);
            // The main check is whether we will have the number of messages (coming from the queue)
            // equal to the max and there will be time to send an email - we can do that.
            if (countMessagesMax == countMessages && isCheckTime()) {
                Transport.send(message);
                System.out.println("Sent message successfully....");
            }

        } catch (MessagingException mex) {
            mex.printStackTrace();
        }

    }

    private boolean isCheckTime() {
        ZoneId zone1 = ZoneId.of("Europe/Bratislava");
        LocalTime time = LocalTime.now(zone1);
        if (time.isAfter(LocalTime.of(12, 8))
            || time.isBefore(LocalTime.of(6, 0))) {
            return true;
        }
        return false;
    }



}
