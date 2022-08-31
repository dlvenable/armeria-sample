package io.venable.samples.armeria.http;

import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.UnprocessedRequestException;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TLSTest {

    private Server server;
    private WebClient webClient;
    private String rootMessage;

    @BeforeEach
    void setUp() {
        rootMessage = "Welcome " + UUID.randomUUID();

        server = Server.builder()
                .https(8443)
                .tls(
                        new File("src/test/resources/cert.pem"),
                        new File("src/test/resources/key.pem"))
                .service("/", (ctx, req) -> HttpResponse.of(rootMessage))
                .build();

        server.start().join();

        final ClientFactory clientFactory = ClientFactory.builder()
                .tlsCustomizer(sslContextBuilder -> sslContextBuilder.trustManager(
                        new File("src/test/resources/cert.pem")
                ))
                .build();
        webClient = WebClient
                .builder("https://localhost:8443")
                .factory(clientFactory)
                .build();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void GET_without_HTTPS_throws() {
        webClient = WebClient
                .of("https://localhost:8443");

        final HttpResponse echoResponse = webClient.get("/");

        final CompletableFuture<AggregatedHttpResponse> aggregate = echoResponse.aggregate();

        final ExecutionException actual = assertThrows(ExecutionException.class, aggregate::get);

        assertThat(actual.getCause(), instanceOf(UnprocessedRequestException.class));
        assertThat(actual.getCause().getCause(), instanceOf(SSLHandshakeException.class));
    }

    @Test
    void GET_over_HTTPS() throws ExecutionException, InterruptedException {
        final HttpResponse echoResponse = webClient.get("/");

        final CompletableFuture<AggregatedHttpResponse> aggregate = echoResponse.aggregate();

        assertThat(aggregate, notNullValue());

        final AggregatedHttpResponse aggregatedHttpResponse = aggregate.get();

        assertThat(aggregatedHttpResponse.status(), equalTo(HttpStatus.OK));
        assertThat(aggregatedHttpResponse.contentUtf8(), equalTo(rootMessage));
    }
}
