package io.narayana.txdemo.tracing;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.Configuration.SenderConfiguration;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

public class TracingUtils {

	private static Tracer tracer = null;
	
	public static Tracer getTracer() {
		if(tracer == null) {
			initializeTracer();
		}
		return tracer;
	}
	
	private static void initializeTracer() {
		if(tracer != null) {
			throw new IllegalStateException("The tracer has already been created and registered. Aborting...");
		}
		SamplerConfiguration samplerConfig = new SamplerConfiguration().withType("const").withParam(1);
		SenderConfiguration senderConfig = new SenderConfiguration().withAgentHost("localhost").withAgentPort(5775);
		ReporterConfiguration reporterConfig = new ReporterConfiguration().withLogSpans(true).withFlushInterval(1000)
				.withMaxQueueSize(10000).withSender(senderConfig);
		tracer = new Configuration("global").withSampler(samplerConfig).withReporter(reporterConfig)
		.getTracer();
		GlobalTracer.registerIfAbsent(tracer);
	}
}
