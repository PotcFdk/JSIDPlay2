package server.restful.common;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.xml.bind.JAXBContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("serial")
public abstract class JSIDPlay2Servlet extends HttpServlet {

	public abstract String getServletPath();

	@SuppressWarnings("unchecked")
	public <T> T getInput(HttpServletRequest request, Class<T> tClass) {
		try (ServletInputStream inputStream = request.getInputStream()) {
			if (request.getContentType() == null
					|| MimeType.MIME_TYPE_JSON.getContentType().startsWith(request.getContentType())) {
				return new ObjectMapper().readValue(inputStream, tClass);
			} else if (MimeType.MIME_TYPE_XML.getContentType().startsWith(request.getContentType())) {
				// MimeType.MIME_XML
				return (T) JAXBContext.newInstance(tClass).createUnmarshaller().unmarshal(inputStream);
			} else {
				throw new RuntimeException("Unsupported content type: " + request.getContentType());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public <T> void setOutput(HttpServletRequest request, HttpServletResponse response, T result, Class<T> tClass) {
		if (result == null) {
			return;
		}
		try (ServletOutputStream out = response.getOutputStream()) {
			if (request.getHeader(HttpHeaders.ACCEPT) == null
					|| MimeType.MIME_TYPE_JSON.getContentType().startsWith(request.getHeader(HttpHeaders.ACCEPT))) {
				response.setContentType(MimeType.MIME_TYPE_JSON.getContentType());
				new ObjectMapper().writeValue(out, result);
			} else if (MimeType.MIME_TYPE_XML.getContentType().startsWith(request.getHeader(HttpHeaders.ACCEPT))) {
				// MimeType.MIME_XML
				response.setContentType(MimeType.MIME_TYPE_XML.getContentType());
				JAXBContext.newInstance(tClass).createMarshaller().marshal(result, out);
			}
			out.flush();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
