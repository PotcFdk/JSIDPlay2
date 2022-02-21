package server.restful.filters;

import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

public final class LimitRequestServletFilter implements Filter {

	private final AtomicInteger atomicServletRequestCounter = new AtomicInteger();

	private final int maxRequestServletCount;

	public LimitRequestServletFilter(int maxRequestServletCount) {
		this.maxRequestServletCount = maxRequestServletCount;
	}

	@Override
	public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
			final FilterChain chain) throws IOException, ServletException {
		try {
			if (atomicServletRequestCounter.getAndIncrement() < maxRequestServletCount) {
				// let the request through and process as usual
				chain.doFilter(servletRequest, servletResponse);
			} else {
				// handle limit case, e.g. return status code 429 (Too Many Requests)
				HttpServletResponse response = (HttpServletResponse) servletResponse;
				response.setContentType(MIME_TYPE_TEXT.toString());
				response.sendError(429, "Too Many Requests");
			}
		} finally {
			atomicServletRequestCounter.getAndDecrement();
		}
	}
}
