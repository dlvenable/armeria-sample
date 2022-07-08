package io.venable.samples.armeria;

import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static Server server;

    static Server newServer(final int port) {
        final ServerBuilder serverBuilder = Server.builder();
        return serverBuilder.http(port)
                .service("/", (ctx, req) -> HttpResponse.of("Hello, Armeria!"))
                .annotatedService("/echo", new EchoService())
                .build();
    }

    public static void main(final String[] args) throws Exception {
        server = newServer(8080);

        server.closeOnJvmShutdown();

        server.start().join();

        log.info("Server has been started. Serving dummy service at http://127.0.0.1:{}",
                server.activeLocalPort());
    }
}
