package io.venable.samples.armeria;

import io.venable.samples.armeria.http.SampleHttpServer;

public class Main {
    private static SampleHttpServer sampleHttpServer;

    public static void main(final String[] args) {
        sampleHttpServer = SampleHttpServer.createServer();
    }
}
