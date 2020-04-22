package server.restful.common;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Objects;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.xml.bind.JAXBContext;

import org.apache.http.entity.ContentType;
import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import com.fasterxml.jackson.databind.ObjectMapper;

import libsidutils.ZipFileUtils;
import sidplay.fingerprinting.WavBean;

@SuppressWarnings("serial")
public abstract class JSIDPlay2Servlet extends HttpServlet {

	public abstract String getServletPath();

	@SuppressWarnings("unchecked")
	public <T> T getInput(HttpServletRequest request, Class<T> tClass) {
		try (ServletInputStream inputStream = request.getInputStream()) {
			String contentType = request.getContentType();
			if (contentType == null || equals(MimeType.MIME_TYPE_JSON, contentType)) {
				return new ObjectMapper().readValue(inputStream, tClass);
			} else if (equals(MimeType.MIME_TYPE_XML, contentType)) {
				return (T) JAXBContext.newInstance(tClass).createUnmarshaller().unmarshal(inputStream);
			} else if (ServletFileUpload.isMultipartContent(request) && tClass.equals(WavBean.class)) {
				// file upload (multipart/mixed)
				StringBuilder result = new StringBuilder();
				result.append("{ \"wav\": \"");
				ServletFileUpload fileUpload = new ServletFileUpload();
				FileItemIterator items = fileUpload.getItemIterator(request);
				while (items.hasNext()) {
					try (InputStream itemInputStream = items.next().openStream()) {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						ZipFileUtils.copy(itemInputStream, bos);
						result.append(Base64.getEncoder().encodeToString(bos.toByteArray()));
					}
					// just the first file
					break;
				}
				result.append("\"}");
				return new ObjectMapper().readValue(result.toString(), tClass);
			} else {
				throw new RuntimeException("Unsupported content type: " + contentType);
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
			String[] acceptedHeaders = request.getHeader(HttpHeaders.ACCEPT).split(",");
			for (String acceptedHeader : acceptedHeaders) {
				if (acceptedHeader == null || equals(MimeType.MIME_TYPE_JSON, acceptedHeader)) {
					response.setContentType(MimeType.MIME_TYPE_JSON.getContentType());
					new ObjectMapper().writeValue(out, result);
					break;
				} else if (equals(MimeType.MIME_TYPE_XML, acceptedHeader)) {
					// MimeType.MIME_XML
					response.setContentType(MimeType.MIME_TYPE_XML.getContentType());
					JAXBContext.newInstance(tClass).createMarshaller().marshal(result, out);
					break;
				}
			}
			out.flush();
		} catch (Exception e) {
			// ignore client aborts
		}
	}

	private boolean equals(MimeType mimeType, String contentTypeString) {
		ContentType contentType = ContentType.parse(contentTypeString);
		if (contentType.getCharset() != null) {
			return Objects.equals(mimeType.getCharset(), contentType.getCharset())
					&& Objects.equals(mimeType.getMimeType(), contentType.getMimeType());
		} else {
			return Objects.equals(mimeType.getMimeType(), contentType.getMimeType());
		}
	}

}
