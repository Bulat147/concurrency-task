package per.khalilov;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final long timeInterval;
    private final int requestLimit;
    private volatile LocalTime lastTime;
    private volatile LocalTime nextTime;
    private final AtomicInteger currentRequestCount = new AtomicInteger(0);
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    public CrptApi(long timeIntervalMillis, int requestLimit) {
        if (requestLimit <= 0) {
            throw new RequestLimitException("Inaccessible limit range - requestLimit must be > 0");
        }
        this.timeInterval = timeIntervalMillis;
        this.requestLimit = requestLimit;
        this.lastTime = LocalTime.now();
        this.nextTime = lastTime.plusNanos(timeInterval * 1_000_000);
        mapper.findAndRegisterModules();
    }

    public String createDocument(Document document) {

        synchronized (this) {
            while (nextTime.isAfter(LocalTime.now()) && currentRequestCount.get() == requestLimit) {
                try {
                    wait(LocalTime.now().until(nextTime, ChronoUnit.MILLIS) + 1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (nextTime.isBefore(LocalTime.now())) {
                lastTime = LocalTime.now();
                nextTime = lastTime.plusNanos(timeInterval * 1_000_000);
                currentRequestCount.set(0);
            }
            currentRequestCount.incrementAndGet();
        }

        String body;
        try {
            body = mapper.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Content-Type", "application/json").build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return response.body();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private LocalDate productionDate;
        private String productionType;
        private List<Product> products;
        private LocalDate regDate;
        private String regNumber;

        @AllArgsConstructor
        @Getter
        @Setter
        class Description {
            private String participantInn;
        }

        @AllArgsConstructor
        @Getter
        @Setter
        class Product {
            private String certificateDocument;
            private LocalDate certificateDocumentDate;
            private String certificateDocumentNumber;
            private String ownerInn;
            private String producerInn;
            private LocalDate productionDate;
            private String tnved_code;
            private String uitCode;
            private String uituCode;

        }
    }

    public class RequestLimitException extends RuntimeException {

        public RequestLimitException(String message) {
            super(message);
        }
    }
}
