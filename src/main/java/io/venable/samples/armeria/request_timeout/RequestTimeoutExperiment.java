package io.venable.samples.armeria.request_timeout;

import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.annotation.Nullable;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.RequestTimeoutException;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServerErrorHandler;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.SimpleDecoratingHttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
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
                .service("/test", (ctx, req) -> HttpResponse.of("Hello, Armeria!"))
                .errorHandler(new CustomServerErrorHandler())
                .decorator(SimpleDecorator.newDecorator())
                .requestTimeout(Duration.of(5, ChronoUnit.SECONDS))
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
                    return HttpResponse.of(HttpStatus.REQUEST_TIMEOUT);
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
                return HttpResponse.of(HttpStatus.REQUEST_TIMEOUT);
            }
            return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
