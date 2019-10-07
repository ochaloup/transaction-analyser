package io.narayana.txdemo.tracing;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

// we specify the name to avoid clash among other tracing startup beans
// present in other libraries
@Singleton(name = "txdemo-tracingstartup")
@Startup
public class TracingStartup {

    @PostConstruct
    private void startTracing() {
        Properties config = TracingHelper.loadConfig();
        Tracer tracer;
        switch (Tracers.valueOf(config.getProperty("tracer").toUpperCase())) {
        case JAEGER:
            tracer = TracingHelper.getJaegerTracer(config);
            break;
        case JAEGER_LOGGED:
            tracer = new TracerLoggingDecorator(TracingHelper.getJaegerTracer(config));
            break;
        default:
            throw new IllegalArgumentException("unsupported tracer type");
        }
        GlobalTracer.registerIfAbsent(tracer);
    }

    private enum Tracers {
        // Standard Jaeger tracer which outputs real spans
        JAEGER,
        // SLF4J decorator useful for debugging
        JAEGER_LOGGED;
    }
}
