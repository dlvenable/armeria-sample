package io.venable.samples.armeria.http;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class SampleHttpServerTest {

    private SampleHttpServer server;
    private WebClient webClient;

    @BeforeEach
    void setUp() {
        server = SampleHttpServer.createServer();

        webClient = WebClient.of("http://127.0.0.1:8080");
    }

    @AfterEach
    void tearDown() {
        server.shutdown();
    }

    @Test
    void POST_echo() throws ExecutionException, InterruptedException {
        final String data = UUID.randomUUID().toString();
        final HttpResponse echoResponse = webClient.post("echo", data);

        assertThat(echoResponse, notNullValue());

        final CompletableFuture<AggregatedHttpResponse> aggregate = echoResponse.aggregate();

        assertThat(aggregate, notNullValue());

        final AggregatedHttpResponse aggregatedHttpResponse = aggregate.get();

        assertThat(aggregatedHttpResponse.status(), equalTo(HttpStatus.OK));
        assertThat(aggregatedHttpResponse.contentUtf8(), equalTo(data));
    }

    @Test
    void GET_root() throws ExecutionException, InterruptedException {


        final HttpResponse echoResponse = webClient.get("/");

        assertThat(echoResponse, notNullValue());

        final CompletableFuture<AggregatedHttpResponse> aggregate = echoResponse.aggregate();

        assertThat(aggregate, notNullValue());

        final AggregatedHttpResponse aggregatedHttpResponse = aggregate.get();

        assertThat(aggregatedHttpResponse.status(), equalTo(HttpStatus.OK));
        assertThat(aggregatedHttpResponse.contentUtf8(), equalTo("Hello, Armeria!"));
    }

}