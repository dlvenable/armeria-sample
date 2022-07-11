package io.venable.samples.armeria.http;

import com.linecorp.armeria.common.AggregatedHttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.annotation.Blocking;
import com.linecorp.armeria.server.annotation.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Blocking
public class EchoService {
    private static final Logger log = LoggerFactory.getLogger(EchoService.class);

    @Post
    public HttpResponse doPost(final AggregatedHttpRequest aggregatedHttpRequest) {
        final String content = aggregatedHttpRequest.contentAscii();

        log.info("Content: {}", content);

        return HttpResponse.builder()
                .content(content)
                .status(200)
                .build();
    }
}
