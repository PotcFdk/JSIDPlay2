package server.restful.filters;

import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

public class LimitRequestFilter implements Filter {

	private final Object lock = new Object();

	private final int limit;
	private int count;

	public LimitRequestFilter(int limit) {
		this.limit = limit;
		count = 0;
	}

	@Override
	public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
			final FilterChain chain) throws IOException, ServletException {
		try {
			// Safe to downcast at this point.
			HttpServletResponse response = (HttpServletResponse) servletResponse;

			boolean ok;
			synchronized (lock) {
				ok = count++ < limit;
			}
			if (ok) {
				// let the request through and process as usual
				chain.doFilter(servletRequest, servletResponse);
			} else {
				// handle limit case, e.g. return status code 429 (Too Many Requests)
				response.setContentType(MIME_TYPE_TEXT.toString());
				// see https://www.rfc-editor.org/rfc/rfc6585#page-3
				response.sendError(429, "Too Many Requests");
			}
		} finally {
			synchronized (lock) {
				count--;
			}
		}
	}
}
