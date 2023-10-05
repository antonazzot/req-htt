package org.example.concurrency.queue.HonestSign;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    public static final String CONTENT_TYPE = "Content-type";
    public static final String TYPE = "application/json";
    private static final int MAX_RETRY_COUNT = 3;
    private final Long DELAY;
    private final HttpClient httpClient;
    private AtomicInteger requestLimit;
    private Queue<HttpPost> requests = new ConcurrentLinkedDeque<>();
    private Long lastSubmit = Long.MAX_VALUE;

    public CrptApi(Integer requestLimit, TimeUnit timeUnit, Long timeUnitAmount) {
        this.requestLimit = requestLimit > 0 ? new AtomicInteger(requestLimit) : new AtomicInteger(0);
        this.DELAY = TimeUnit.MILLISECONDS.convert(timeUnitAmount, timeUnit);
        this.httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(30000)
                        .setSocketTimeout(30000).build())
                .build();
    }

    private void prepareRequest(Document document, String sing) {
        HttpPost request = new HttpPost(Properties.loadFromProperties());
        request.setHeader(CONTENT_TYPE, TYPE);
        try {
            request.setEntity(new StringEntity(JacksonObjectMapperHolder.INSTANCE.getObjectMapper().writeValueAsString(document)));
            requests.add(request);
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            // for instance log and rethrow
            e.printStackTrace(System.out);
        }
    }

    public void sendDocument(Document document, String sing) {
        if (getRequestLimit() > 0) {
            prepareRequest(document, sing);
            while (getRequestLimit() > 0) {
                sendOne();
            }
        }
    }

    private synchronized void sendOne() {
        if (System.currentTimeMillis() - lastSubmit > DELAY && !CollectionUtils.isEmpty(requests)) {
            int retryCount = 0;
            try {
                httpClient.execute(requests.poll());
                lastSubmit = System.currentTimeMillis();
                requestLimit.decrementAndGet();
            } catch (IOException e) {
                retryCount++;
                sendOne();
            }
        }
    }

    public Integer getRequestLimit() {
        return requestLimit.get();
    }



    public class Properties {
        private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

        public static  String loadFromProperties(){
            return API_URL;
        }
    }



    public final class JacksonObjectMapperHolder {

        private static volatile JacksonObjectMapperHolder INSTANCE;

        private static final Object MUTEX = new Object();

        public  JacksonObjectMapperHolder getInstance() {
            JacksonObjectMapperHolder instance = INSTANCE;

            if(instance == null) {
                synchronized(MUTEX) {
                    instance = INSTANCE;

                    if(instance == null) {
                        INSTANCE = instance = new JacksonObjectMapperHolder();
                    }
                }
            }

            return instance;
        }

        private final ObjectMapper objectMapper = new ObjectMapper();

        private  JacksonObjectMapperHolder() {
            super();
        }

        public  ObjectMapper getObjectMapper() {
            return objectMapper;
        }

    }

    public class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;
    }


    public class Description {
        private String participantInn;

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }
    }

    public class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;

        public String getCertificate_document() {
            return certificate_document;
        }

        public void setCertificate_document(String certificate_document) {
            this.certificate_document = certificate_document;
        }

        public String getCertificate_document_date() {
            return certificate_document_date;
        }

        public void setCertificate_document_date(String certificate_document_date) {
            this.certificate_document_date = certificate_document_date;
        }

        public String getCertificate_document_number() {
            return certificate_document_number;
        }

        public void setCertificate_document_number(String certificate_document_number) {
            this.certificate_document_number = certificate_document_number;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public String getProduction_date() {
            return production_date;
        }

        public void setProduction_date(String production_date) {
            this.production_date = production_date;
        }

        public String getTnved_code() {
            return tnved_code;
        }

        public void setTnved_code(String tnved_code) {
            this.tnved_code = tnved_code;
        }

        public String getUit_code() {
            return uit_code;
        }

        public void setUit_code(String uit_code) {
            this.uit_code = uit_code;
        }

        public String getUitu_code() {
            return uitu_code;
        }

        public void setUitu_code(String uitu_code) {
            this.uitu_code = uitu_code;
        }
    }



}


