package io.venable.samples.armeria;

import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.SimpleDecoratingHttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class CopyDecorator extends SimpleDecoratingHttpService {
    private static final Logger log = LoggerFactory.getLogger(CopyDecorator.class);

    private CopyDecorator(HttpService httpService) {
        super(httpService);
    }

    public static Function<? super HttpService, CopyDecorator> newDecorator() {
        return CopyDecorator::new;
    }

    @Override
    public HttpResponse serve(final ServiceRequestContext ctx, final HttpRequest req) throws Exception {
        log.info("In decorator");
        final String content = req.aggregate().get().content().toStringAscii();
        log.info("Content: {}", content);

        final HttpRequest newRequest = HttpRequest.builder()
                .content(content)
                .headers(req.headers())
                .path(req.path())
                .method(req.method())
                .build();
        return unwrap().serve(ctx, newRequest);
    }
}
