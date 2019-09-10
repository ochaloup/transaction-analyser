package io.narayana.txdemo.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

class TracerLoggingDecorator implements Tracer {
	
	private final Logger logger = LoggerFactory.getLogger(TracerLoggingDecorator.class);
	private final Tracer tracer;

	public TracerLoggingDecorator(Tracer tracer) {
		this.tracer = tracer;
	}
	
	@Override
	public ScopeManager scopeManager() {
		return tracer.scopeManager();
	}

	@Override
	public Span activeSpan() {
		Span s = tracer.activeSpan();
		logger.trace(String.format("retrieving active span '%s'", s == null ? "N/A" : s.toString()));
		return s;
	}

	@Override
	public Scope activateSpan(Span span) {
		logger.debug(String.format("activating span '%s'", span == null ? "N/A" : span.toString()));
		return tracer.activateSpan(span);
	}

	@Override
	public SpanBuilder buildSpan(String operationName) {
		return tracer.buildSpan(operationName);
	}

	@Override
	public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
	    tracer.inject(spanContext, format, carrier);
	}

	@Override
	public <C> SpanContext extract(Format<C> format, C carrier) {
		return tracer.extract(format, carrier);
	}

	@Override
	public void close() {
		tracer.close();
	}
}
