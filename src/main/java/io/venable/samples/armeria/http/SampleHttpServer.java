package io.venable.samples.armeria.http;

import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import io.venable.samples.armeria.CopyDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleHttpServer {
    private static final Logger log = LoggerFactory.getLogger(SampleHttpServer.class);
    private final Server server;

    private SampleHttpServer(final Server server) {
        this.server = server;
    }

    private static Server newServer(final int port) {
        final ServerBuilder serverBuilder = Server.builder();
        return serverBuilder.http(port)
                .service("/", (ctx, req) -> HttpResponse.of("Hello, Armeria!"))
                .decorator(CopyDecorator.newDecorator())
                .annotatedService("/echo", new EchoService())
                .build();
    }

    public static SampleHttpServer createServer() {
        final Server server = newServer(8080);

        //server.closeOnJvmShutdown();

        server.start().join();

        log.info("Server has been started. Serving dummy service at http://127.0.0.1:{}",
                server.activeLocalPort());

        return new SampleHttpServer(server);
    }

    void shutdown() {
        server.stop();
    }
}
