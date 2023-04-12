package com.kyndryl.issp.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kyndryl.issp.config.KindOfQueue;


@Service
public class RabbitMQHelper {

    @Value("${spring.rabbitmq.addresses}")
    String address;

    @Value("${spring.rabbitmq.username}")
    String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${issp.env}")
    private String env;

    @Value("${teams.webhook}")
    private String webhookUrl;

    private final String WHITE_SPACE = " ";

    private int lastCountMessages_I = 0;
    private int lastCountMessages_D = 0;
    private int lastCountMessages_P = 0;

    public String queueName;
    public String exchange;
    public String routing_key;

    private int queueMessageCount;
    private int queueConsumerCount;

   private AmqpAdmin amqpAdmin;

    @Autowired
    public RabbitMQHelper(AmqpAdmin amqpAdmin ) {
        this.amqpAdmin = amqpAdmin;
    }

    public String getQueuePropAsMessage(String param) {
        selectQueue(param);

        Properties p = amqpAdmin.getQueueProperties(queueName);
        for (Map.Entry<Object, Object> e : p.entrySet()) {
            if ("QUEUE_NAME".equals(e.getKey())) {
                queueName = (String) e.getValue();
            }
            if ("QUEUE_MESSAGE_COUNT".equals(e.getKey())) {
                queueMessageCount = (int) e.getValue();;
            }
            if ("QUEUE_CONSUMER_COUNT".equals(e.getKey())) {
                queueConsumerCount = (int) e.getValue();
            }
        }
        StringBuilder sb = new StringBuilder("For queue name: " + queueName.toUpperCase() + " we have in queue message container now: " + queueMessageCount
            + " messages If this count will be over the limit of 250, we need to check this queue message container.");
        return sb.toString();
    }


    public void selectQueue(String param) {
        switch (param) {
            case "I":
                queueName = "upload.cases.in";
                exchange = "issp.uploading";
                routing_key = "upload.in";
                break;
            case "D":
                queueName = "upload.cases.done";
                exchange = "issp.uploading";
                routing_key = "upload.done";
                break;
            case "P":
                queueName = "upload.caseparts.in";
                exchange = "issp.uploading";
                routing_key = "upload.txin";
                break;
            default:
                queueName = "upload.cases.in";
                exchange = "issp.uploading";
                routing_key = "upload.in";
                break;
        }
    }

    public String sendRequest (String param) throws IOException {
        selectQueue(param);
        String encoding = Base64.getEncoder().encodeToString((username + ":" + password).getBytes("utf-8") );

        URL url = new URL("https://"+address+"/api/queues/%2F/"+queueName);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("GET");
        httpConn.setRequestProperty("Accept-Encoding", "application/json");
        httpConn.setRequestProperty("Authorization", "Basic " + encoding);

        int responseCode = httpConn.getResponseCode();
        System.out.println("GET Response Code :: " + responseCode);

        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
            ? httpConn.getInputStream()
            : httpConn.getErrorStream();

        StringBuilder responseStrBuilder = new StringBuilder();
        String inputStr;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(responseStream));
        while ((inputStr = bufferedReader.readLine()) != null)
        {
            responseStrBuilder.append(inputStr);
        }
        JSONObject jsonObject = new JSONObject(responseStrBuilder.toString());
       queueMessageCount = jsonObject.getInt("messages_unacknowledged_ram");
       // queueMessageCount = jsonObject.getInt("messages_ready_ram");
       // queueMessageCount = jsonObject.getInt("messages_ram");
        bufferedReader.close();

        StringBuilder sb = new StringBuilder("For queue name: " + queueName.toUpperCase() + " we have messages unacked in the queue container: " + queueMessageCount
            + " messages. If this number of messages is the same over a time frame of one or more hours, it means that we need to do something.");
        return sb.toString();
    }


    public JsonNode prepareData() throws IOException, ParseException {
        File file = ResourceUtils.getFile("adaptiveCard_template.json");
        String content = new String(Files.readAllBytes(file.toPath()));

        ObjectMapper mapper = new JsonMapper();
        JsonNode json = mapper.readTree(content);

        ArrayNode attachments = (ArrayNode) json.get("attachments");
        List<ArrayNode> list = Arrays.asList(attachments);
        ObjectNode con = (ObjectNode) list.get(0).get(0).get("content");
        ArrayNode arrayNode = (ArrayNode) con.get("body");

        for (JsonNode jsonNode : arrayNode) {
            String id = jsonNode.get("id").asText();

            if ("TextBlock".equals(id)) {
                String oldText = jsonNode.get("text").asText();
                String newText = oldText.replace("UAT", "ISSP "+env);
                ((ObjectNode) jsonNode).put("text", newText);
                continue;
            }

            if ("RichTextBlock_1".equals(id)) {
                 selectQueue(KindOfQueue.PARAM_UCI.kind);
                 sendRequest(KindOfQueue.PARAM_UCI.kind);

                ArrayNode children = (ArrayNode) jsonNode.get("inlines");
                for (JsonNode jn : children) {
                    String children_id = jn.get("id").asText();

                    if ("TextRun_2".equals(children_id)) {
                        ((ObjectNode) jn).put("text", queueName + WHITE_SPACE);
                    }

                    if ("TextRun_4".equals(children_id)) {
                        ((ObjectNode) jn).put("text", getQueueMessageCount() + WHITE_SPACE);
                    }
                }
                continue;
            }

            if ("RichTextBlock_2".equals(id)) {
                selectQueue(KindOfQueue.PARAM_UCD.kind);
                sendRequest(KindOfQueue.PARAM_UCD.kind);
                ArrayNode children = (ArrayNode) jsonNode.get("inlines");
                for (JsonNode jn : children) {
                    String children_id = jn.get("id").asText();

                    if ("TextRun_2".equals(children_id)) {
                        ((ObjectNode) jn).put("text", queueName + WHITE_SPACE);
                    }

                    if ("TextRun_4".equals(children_id)) {
                        ((ObjectNode) jn).put("text", getQueueMessageCount() + WHITE_SPACE);
                    }
                }
                continue;
            }
            if ("RichTextBlock_3".equals(id)) {
                selectQueue(KindOfQueue.PARAM_UCP.kind);
                sendRequest(KindOfQueue.PARAM_UCP.kind);
                ArrayNode children = (ArrayNode) jsonNode.get("inlines");
                for (JsonNode jn : children) {
                    String children_id = jn.get("id").asText();

                    if ("TextRun_2".equals(children_id)) {
                        ((ObjectNode) jn).put("text", queueName + WHITE_SPACE);
                    }

                    if ("TextRun_4".equals(children_id)) {
                        ((ObjectNode) jn).put("text", getQueueMessageCount() + WHITE_SPACE);
                    }
                }
                continue;
            }

        }
        return json;
    }

    public String sendRequest(JsonNode json) throws IOException {
        HttpHeaders header = new HttpHeaders();
        header.set("Content-Type", "application/json");

        HttpEntity entity = new HttpEntity(json, header);
        ResponseEntity<String> response = getRestTemplate().exchange(webhookUrl, HttpMethod.POST, entity, String.class);

        if (response.getStatusCodeValue() != 200) {
            throw new IOException(response.toString());
        }
        return null;
    }

    private RestTemplate getRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        List<HttpMessageConverter<?>> httpMessageConverters = getHttpMessageConverters();
        restTemplate.setMessageConverters(httpMessageConverters);
        return restTemplate;
    }

    private List<HttpMessageConverter<?>> getHttpMessageConverters() {
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
        stringConverter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
        MappingJackson2HttpMessageConverter jackson2Converter = new MappingJackson2HttpMessageConverter();
        jackson2Converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
        return new ArrayList<>(Arrays.asList(jackson2Converter, stringConverter));
    }

    public String getQueueName() {
        return queueName;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRouting_key() {
        return routing_key;
    }

    public int getQueueMessageCount() {
        return queueMessageCount;
    }

    public int getLastCountMessages_I() {
        return lastCountMessages_I;
    }

    public void setLastCountMessages_I(int lastCountMessages_I) {
        this.lastCountMessages_I = lastCountMessages_I;
    }

    public int getLastCountMessages_D() {
        return lastCountMessages_D;
    }

    public void setLastCountMessages_D(int lastCountMessages_D) {
        this.lastCountMessages_D = lastCountMessages_D;
    }

    public int getLastCountMessages_P() {
        return lastCountMessages_P;
    }

    public void setLastCountMessages_P(int lastCountMessages_P) {
        this.lastCountMessages_P = lastCountMessages_P;
    }
}
