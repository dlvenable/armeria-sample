package io.venable.samples.armeria.request_timeout;

import com.linecorp.armeria.common.AggregatedHttpRequest;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.common.annotation.Nullable;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.RequestTimeoutException;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServerErrorHandler;
import com.linecorp.armeria.server.Service;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.SimpleDecoratingHttpService;
import com.linecorp.armeria.server.annotation.ExceptionHandlerFunction;
import com.linecorp.armeria.server.annotation.Post;
import com.linecorp.armeria.server.throttling.ThrottlingRejectHandler;
import com.linecorp.armeria.server.throttling.ThrottlingService;
import com.linecorp.armeria.server.throttling.ThrottlingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * Creates a server with a decorator and a defined request timeout.
 * This server tries to control the response when the request times out,
 * but it is resulting in a 503 Service Unavailable.
 * <p>
 * Run the main method from IntelliJ.
 * Run:
 * <pre>
 * curl -v http://localhost:8080/test -X POST -H 'Content-Type: application/json' -H 'Content-Length: 100'
 * </pre>
 */
public class RequestTimeoutExperiment {
    private static final Logger log = LoggerFactory.getLogger(RequestTimeoutExperiment.class);
    private static RequestTimeoutExperiment requestTimeoutExperiment;

    public static void main(final String[] args) {
        requestTimeoutExperiment = RequestTimeoutExperiment.createServer();
    }

    private final Server server;

    private RequestTimeoutExperiment(final Server server) {
        this.server = server;
    }

    private static Server newServer(final int port) {
        final ServerBuilder serverBuilder = Server.builder();
        return serverBuilder.http(port)
                //.service("/test", (ctx, req) -> HttpResponse.of("Hello, Armeria!"))
                //.decorator("/test", SimpleDecorator.newDecorator())
                // TODO: Remove the following line to get the 408 that we'd expect.
                .decorator(ThrottlingService.newDecorator(ThrottlingStrategy.rateLimiting(10.0), new CustomThrottlingRejectHandler()))
                .annotatedService("/test", new MyService())
                //.errorHandler(new CustomServerErrorHandler())
                //.requestTimeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();
    }

    private static RequestTimeoutExperiment createServer() {
        final Server server = newServer(8080);

        server.start().join();

        log.info("Server has been started. Serving request timeout experiment service at http://127.0.0.1:{}",
                server.activeLocalPort());

        return new RequestTimeoutExperiment(server);
    }

    private static class SimpleDecorator extends SimpleDecoratingHttpService {
        private SimpleDecorator(HttpService httpService) {
            super(httpService);
        }

        public static Function<? super HttpService, SimpleDecorator> newDecorator() {
            return SimpleDecorator::new;
        }

        @Override
        public HttpResponse serve(final ServiceRequestContext ctx, final HttpRequest req) throws Exception {
            return HttpResponse.from(req.aggregate().handle((aggregatedHttpRequest, throwable) -> {
                log.info("In decorator");
                if(throwable != null) {
                    log.error("Error in decorator", throwable);
                    // Returning a different response to see if it is modified
                    return HttpResponse.of(HttpStatus.BAD_REQUEST);
                }

                try {
                    return unwrap().serve(ctx, aggregatedHttpRequest.toHttpRequest());
                } catch (Exception e) {
                    return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                }
            }));
        }
    }

    private static class CustomServerErrorHandler implements ServerErrorHandler {
        @Override
        public @Nullable HttpResponse onServiceException(ServiceRequestContext ctx, Throwable cause) {
            if(cause instanceof RequestTimeoutException) {
                log.info("Handling RequestTimeoutException");
                return HttpResponse.of(HttpStatus.REQUEST_TIMEOUT);
            }
            log.info("Handling any other error.");
            return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static class MyService {
        @Post
        public HttpResponse doPost(final ServiceRequestContext serviceRequestContext, final AggregatedHttpRequest aggregatedHttpRequest) throws Exception {
            return HttpResponse.of("Hello, Armeria!");
        }
    }

    private static class CustomExceptionHandlerFunction implements ExceptionHandlerFunction {
        @Override
        public HttpResponse handleException(ServiceRequestContext ctx, HttpRequest req, Throwable cause) {
            log.error("Error in decorator", cause);
            return HttpResponse.of(HttpStatus.OK, MediaType.ANY_TYPE, "Error handled");
        }
    }

    private static class CustomThrottlingRejectHandler implements ThrottlingRejectHandler<HttpRequest, HttpResponse> {

        @Override
        public HttpResponse handleRejected(Service<HttpRequest, HttpResponse> delegate, ServiceRequestContext ctx, HttpRequest req, @Nullable Throwable cause) throws Exception {
            return HttpResponse.of(HttpStatus.TOO_MANY_REQUESTS, MediaType.ANY_TYPE,
                    "Not completed"
            );
        }
    }
}
