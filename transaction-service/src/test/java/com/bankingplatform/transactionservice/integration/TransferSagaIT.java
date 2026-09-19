package com.bankingplatform.transactionservice.integration;

import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.constants.SecurityConstants;
import com.bankingplatform.common.dto.AccountBalanceResponse;
import com.bankingplatform.common.event.TransactionEvent;
import com.bankingplatform.common.event.TransactionEventType;
import com.bankingplatform.transactionservice.dto.TransactionResponse;
import com.bankingplatform.transactionservice.dto.TransferRequest;
import com.bankingplatform.transactionservice.entity.TransactionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end test of the fund-transfer Saga against real infrastructure:
 * a real MySQL (so Flyway's actual migration scripts run, and Hibernate
 * validates entities against the real resulting schema, for the first time
 * anywhere in this project outside of manual review) and a real Kafka
 * broker (so the outbox-relay -> topic path is genuinely exercised, not
 * assumed). Account Service itself is stubbed with WireMock rather than
 * run as a real container: this test's job is to prove Transaction
 * Service's own orchestration, persistence, and event-publishing wiring
 * is correct, not to re-verify Account Service's behavior (that's Account
 * Service's own test suite's job) — the AccountServiceClient Feign
 * contract is the seam between the two, and WireMock lets this test
 * exercise the real Feign + Resilience4j client against that seam instead
 * of bypassing it with a mock bean.
 *
 * Named "*IT" (not "*Test") on purpose: Surefire's default excludes skip
 * *IT classes so `mvn test` stays fast, and Failsafe (bound to
 * integration-test/verify in this module's pom.xml) picks them up
 * instead — these tests need Docker for Testcontainers and take
 * meaningfully longer than the mocked unit tests in
 * TransferServiceImplTest, so they're kept out of the fast inner loop.
 *
 * Complements, rather than duplicates, TransferServiceImplTest: the unit
 * test already covers every orchestration branch (including the
 * compensation-itself-fails worst case) against a mocked
 * AccountServiceGateway. This test covers the two scenarios worth proving
 * against real infrastructure — the happy path and the compensation path —
 * plus one the unit test can't reach at all: idempotent replay through the
 * real database's unique constraint, over real HTTP.
 */
@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // No Eureka/Config Server in this test: the Feign call under test
                // is redirected straight at WireMock below, and every other
                // property those two would normally supply has a local default
                // already baked into application.yml.
                "eureka.client.enabled=false",
                "spring.cloud.config.enabled=false",
                // security.jwt.secret and security.internal.api-key have no
                // defaults in application.yml (they normally only ever come from
                // Config Server), so they must be supplied here or the context
                // fails to start. Keep these two values in sync with
                // mintCustomerJwt(...) and the WireMock header assertions below.
                "security.jwt.secret=integration-test-secret-key-must-be-at-least-256-bits-long-0123456789",
                "security.internal.api-key=test-internal-api-key",
                // Faster than the 1s production default, so tests aren't spent
                // idling on the outbox poll's fixedDelay.
                "outbox.poll-interval-ms=300"
        })
class TransferSagaIT {

    private static final String JWT_SECRET =
            "integration-test-secret-key-must-be-at-least-256-bits-long-0123456789";
    private static final String INTERNAL_API_KEY = "test-internal-api-key";

    // Managed manually (started in a static initializer, not via
    // @AutoConfigureWireMock) so its dynamic port is available the moment the
    // class loads -- before Spring's context construction, and therefore
    // safely before @DynamicPropertySource below ever reads it.
    private static final WireMockServer WIREMOCK =
            new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    static {
        WIREMOCK.start();
    }

    @Container
    static MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("transaction_db")
            .withUsername("test")
            .withPassword("test")
            // Matches the query params baked into every service's production
            // JDBC URL (see application.yml). Without allowPublicKeyRetrieval,
            // MySQL 8's default caching_sha2_password auth plugin can reject
            // the very first connection with "Public Key Retrieval is not
            // allowed" -- relying on whatever Testcontainers' own default
            // getJdbcUrl() happens to include isn't worth the risk here.
            .withUrlParam("useSSL", "false")
            .withUrlParam("allowPublicKeyRetrieval", "true")
            .withUrlParam("serverTimezone", "UTC");

    // Confluent's image, not apache/kafka (the image docker-compose.yml and
    // the k8s manifests use): Testcontainers' KafkaContainer is built around
    // the Confluent Platform image family, and swapping images here would
    // risk its startup scripts not matching. What matters for this test is
    // wire-protocol compatibility with the spring-kafka client under test,
    // which cp-kafka provides identically -- a different Kafka distribution
    // in the test harness than in the deployed stack is a common, low-risk
    // trade, not a real inconsistency.
    @Container
    static KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.3"));

    private static KafkaConsumer<String, String> eventConsumer;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        // Points the AccountServiceClient Feign client straight at WireMock,
        // bypassing Eureka/LoadBalancer resolution entirely for this one
        // client (contextId matches AccountServiceClient's @FeignClient).
        registry.add("feign.client.config.accountServiceInternalClient.url",
                () -> "http://localhost:" + WIREMOCK.port());
    }

    @BeforeAll
    static void setUpKafkaConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "transfer-saga-it-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        eventConsumer = new KafkaConsumer<>(props);
        eventConsumer.subscribe(List.of(KafkaTopics.TRANSACTION_EVENTS));
    }

    @AfterAll
    static void tearDownKafkaConsumer() {
        if (eventConsumer != null) {
            eventConsumer.close();
        }
        WIREMOCK.stop();
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void resetWireMock() {
        WIREMOCK.resetAll();
    }

    @Test
    @DisplayName("Happy path: debit + credit both succeed -> COMPLETED, event published to real Kafka")
    void transfer_happyPath_completesAndPublishesEvent() throws Exception {
        String fromAccountId = "acc-from-" + UUID.randomUUID();
        String toAccountId = "acc-to-" + UUID.randomUUID();

        WIREMOCK.stubFor(post(urlEqualTo("/accounts/internal/debit"))
                .willReturn(okJson(objectMapper.writeValueAsString(balance(fromAccountId, "900.00")))));
        WIREMOCK.stubFor(post(urlEqualTo("/accounts/internal/credit"))
                .willReturn(okJson(objectMapper.writeValueAsString(balance(toAccountId, "1100.00")))));

        ResponseEntity<TransactionResponse> response = postTransfer(fromAccountId, toAccountId,
                new BigDecimal("100.00"), UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.getBody().getFailureReason()).isNull();

        // Real MySQL round trip: this is only reachable if Flyway's actual
        // V1__init_schema.sql ran cleanly against a real database and
        // Hibernate's entity mapping validated against the result.
        String transactionId = response.getBody().getId();
        ResponseEntity<TransactionResponse> fetched = restTemplate.exchange(
                "/transactions/" + transactionId, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), TransactionResponse.class);
        assertThat(fetched.getBody().getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        // Real Kafka round trip: proves OutboxPublisher's poll-and-send loop
        // actually delivered the event, not just that a row was written.
        TransactionEvent published = awaitTransactionEvent(transactionId, TransactionEventType.TRANSFER_COMPLETED);
        assertThat(published.getFromAccountId()).isEqualTo(fromAccountId);
        assertThat(published.getToAccountId()).isEqualTo(toAccountId);
        assertThat(published.getAmount()).isEqualByComparingTo("100.00");

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo("/accounts/internal/debit"))
                .withHeader(SecurityConstants.INTERNAL_API_KEY_HEADER, equalTo(INTERNAL_API_KEY)));
        WIREMOCK.verify(1, postRequestedFor(urlEqualTo("/accounts/internal/credit")));
        WIREMOCK.verify(0, postRequestedFor(urlEqualTo("/accounts/internal/compensate")));
    }

    @Test
    @DisplayName("Credit leg fails: debit is compensated -> COMPENSATED, compensation event published")
    void transfer_creditFails_compensatesDebit() throws Exception {
        String fromAccountId = "acc-from-" + UUID.randomUUID();
        String toAccountId = "acc-to-" + UUID.randomUUID();

        WIREMOCK.stubFor(post(urlEqualTo("/accounts/internal/debit"))
                .willReturn(okJson(objectMapper.writeValueAsString(balance(fromAccountId, "900.00")))));
        // Simulates the destination account being frozen/invalid -- any
        // non-2xx is enough to make the Feign call throw.
        WIREMOCK.stubFor(post(urlEqualTo("/accounts/internal/credit"))
                .willReturn(aResponse().withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"account frozen\"}")));
        WIREMOCK.stubFor(post(urlEqualTo("/accounts/internal/compensate"))
                .willReturn(okJson(objectMapper.writeValueAsString(balance(fromAccountId, "1000.00")))));

        ResponseEntity<TransactionResponse> response = postTransfer(fromAccountId, toAccountId,
                new BigDecimal("250.00"), UUID.randomUUID().toString());

        // The saga handled its own failure: this is still an HTTP 200
        // describing a COMPENSATED outcome, not an HTTP error.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(TransactionStatus.COMPENSATED);
        assertThat(response.getBody().getFailureReason()).contains("Credit failed");

        TransactionEvent published = awaitTransactionEvent(response.getBody().getId(), TransactionEventType.TRANSFER_COMPENSATED);
        assertThat(published.getReason()).contains("Credit failed");

        // Not asserting an exact count on /credit: Resilience4j's default
        // Retry (3 attempts) means WireMock legitimately sees more than one
        // call there, and coupling this test to that retry count would just
        // make it brittle without proving anything more about the saga
        // itself. What matters is that it was attempted, and that exactly
        // one compensating reversal followed.
        WIREMOCK.verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/accounts/internal/credit")));
        WIREMOCK.verify(1, postRequestedFor(urlEqualTo("/accounts/internal/compensate")));
    }

    @Test
    @DisplayName("Idempotent replay: repeating the same idempotencyKey never re-triggers account calls")
    void transfer_idempotentReplay_doesNotRepeatSideEffects() throws Exception {
        String fromAccountId = "acc-from-" + UUID.randomUUID();
        String toAccountId = "acc-to-" + UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();

        WIREMOCK.stubFor(post(urlEqualTo("/accounts/internal/debit"))
                .willReturn(okJson(objectMapper.writeValueAsString(balance(fromAccountId, "800.00")))));
        WIREMOCK.stubFor(post(urlEqualTo("/accounts/internal/credit"))
                .willReturn(okJson(objectMapper.writeValueAsString(balance(toAccountId, "1200.00")))));

        ResponseEntity<TransactionResponse> first = postTransfer(fromAccountId, toAccountId,
                new BigDecimal("200.00"), idempotencyKey);
        ResponseEntity<TransactionResponse> second = postTransfer(fromAccountId, toAccountId,
                new BigDecimal("200.00"), idempotencyKey);

        assertThat(first.getBody().getId()).isEqualTo(second.getBody().getId());
        assertThat(second.getBody().getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        // The real assertion: the SECOND HTTP call, against the real
        // idempotency_key unique index, never touched Account Service again.
        WIREMOCK.verify(1, postRequestedFor(urlEqualTo("/accounts/internal/debit")));
        WIREMOCK.verify(1, postRequestedFor(urlEqualTo("/accounts/internal/credit")));
    }

    // -- helpers --------------------------------------------------------------

    private ResponseEntity<TransactionResponse> postTransfer(String fromAccountId, String toAccountId,
                                                               BigDecimal amount, String idempotencyKey) {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(amount)
                .currency("INR")
                .idempotencyKey(idempotencyKey)
                .build();
        return restTemplate.postForEntity("/transactions/transfer",
                new HttpEntity<>(request, authHeaders()), TransactionResponse.class);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(mintCustomerJwt("user-" + UUID.randomUUID()));
        return headers;
    }

    private AccountBalanceResponse balance(String accountId, String amount) {
        return AccountBalanceResponse.builder()
                .accountId(accountId)
                .balance(new BigDecimal(amount))
                .currency("INR")
                .status("ACTIVE")
                .build();
    }

    /** Mints a token this service's own NimbusJwtDecoder will accept: same
     *  HS256 algorithm, same secret, same "roles" claim name/shape that
     *  JwtAuthenticationConverter expects (see SecurityConfig). Built with
     *  Nimbus directly (already on the classpath transitively via
     *  spring-boot-starter-oauth2-resource-server, which is what
     *  NimbusJwtDecoder itself is built on) rather than adding yet another
     *  JWT library just for test token minting. */
    private String mintCustomerJwt(String userId) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(userId)
                    .claim("username", userId)
                    .claim(SecurityConstants.ROLE_CLAIM, List.of(SecurityConstants.ROLE_CUSTOMER))
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + Duration.ofMinutes(15).toMillis()))
                    .jwtID(UUID.randomUUID().toString())
                    .build();
            SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJwt.sign(new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
            return signedJwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to mint test JWT", e);
        }
    }

    /** Polls the real transaction-events topic until a matching event shows
     *  up (or fails the test after the timeout) -- proof that
     *  OutboxPublisher's poll-and-send loop actually ran against this
     *  Testcontainers-backed broker, not just that a PENDING outbox row
     *  exists in the database. */
    private TransactionEvent awaitTransactionEvent(String transactionId, TransactionEventType eventType) {
        List<TransactionEvent> collected = new ArrayList<>();
        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    ConsumerRecords<String, String> records = eventConsumer.poll(Duration.ofMillis(300));
                    for (ConsumerRecord<String, String> record : records) {
                        collected.add(objectMapper.readValue(record.value(), TransactionEvent.class));
                    }
                    assertThat(collected).anyMatch(e ->
                            e.getTransactionId().equals(transactionId) && e.getEventType() == eventType);
                });
        return collected.stream()
                .filter(e -> e.getTransactionId().equals(transactionId) && e.getEventType() == eventType)
                .findFirst()
                .orElseThrow();
    }
}
