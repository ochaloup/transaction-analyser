package io.narayana.txdemo.tracing;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.Configuration.SenderConfiguration;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

public class TracingUtils {

	private static Tracer tracer = null;
	private static final String TRACER_CONFIG_LOCATION = "tracer_config.properties";

	public static Tracer getTracer() {
		if (tracer == null) {
			initializeTracer(loadConfig());
		}
		return tracer;
	}

	private enum Tracers {
		// Standard Jaeger tracer which outputs real spans
		JAEGER,
		// SLF4J decorator useful for debugging
		JAEGER_LOGGED;
	}

	private static void initializeTracer(Properties config) {
		if (tracer != null) {
			throw new IllegalStateException("The tracer has already been created and registered. Aborting...");
		}
		switch (Tracers.valueOf(config.getProperty("tracer").toUpperCase())) {
		case JAEGER:
			tracer = getJaegerTracer(config);
			break;
		case JAEGER_LOGGED:
			tracer = new TracerLoggingDecorator(getJaegerTracer(config));
			break;
		default:
			throw new IllegalArgumentException("unsupported tracer type");
		}
		GlobalTracer.registerIfAbsent(tracer);
	}
	
	static Tracer getJaegerTracer(Properties config) {
		SamplerConfiguration samplerConfig = new SamplerConfiguration().withType("const").withParam(1);
		SenderConfiguration senderConfig = new SenderConfiguration()
				.withAgentHost(config.getProperty("jaeger.reporter_host"))
				.withAgentPort(Integer.decode(config.getProperty("jaeger.reporter_port")));
		ReporterConfiguration reporterConfig = new ReporterConfiguration().withLogSpans(true).withFlushInterval(1000)
				.withMaxQueueSize(10000).withSender(senderConfig);
		return new Configuration("global").withSampler(samplerConfig).withReporter(reporterConfig).getTracer();
	}

	private static Properties loadConfig() {
		try (InputStream fs = TracingUtils.class.getClassLoader().getResourceAsStream(TRACER_CONFIG_LOCATION)) {
			Properties config = new Properties();
			config.load(fs);
			return config;
		} catch (IOException ex) {
			// unrecoverable exception
			throw new RuntimeException(ex);
		}
	}
}
