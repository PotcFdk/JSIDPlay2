package server.restful.common;

import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_XML;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.HttpHeaders;
import javax.xml.bind.JAXBContext;

import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.ZipFileUtils;

@SuppressWarnings("serial")
public abstract class JSIDPlay2Servlet extends HttpServlet {

	protected static final String C64_MUSIC = "/C64Music";
	protected static final String CGSC = "/CGSC";

	protected ServletUtil util;

	public JSIDPlay2Servlet(ServletUtil util) {
		this.util = util;
	}

	public abstract String getServletPath();

	@SuppressWarnings("unchecked")
	public <T> T getInput(HttpServletRequest request, Class<T> tClass) {
		try (ServletInputStream inputStream = request.getInputStream()) {
			String contentType = request.getContentType();
			if (contentType == null || MIME_TYPE_JSON.isCompatible(contentType)) {
				return new ObjectMapper().readValue(inputStream, tClass);
			} else if (MIME_TYPE_XML.isCompatible(contentType)) {
				return (T) JAXBContext.newInstance(tClass).createUnmarshaller().unmarshal(inputStream);
			} else if (ServletFileUpload.isMultipartContent(request)) {
				// file upload (multipart/mixed)
				ByteArrayOutputStream result = new ByteArrayOutputStream();
				ServletFileUpload fileUpload = new ServletFileUpload();
				FileItemIterator items = fileUpload.getItemIterator(request);
				while (items.hasNext()) {
					try (InputStream itemInputStream = items.next().openStream()) {
						ZipFileUtils.copy(itemInputStream, result);
					}
					// just the first file
					break;
				}
				return tClass.getConstructor(new Class[] { byte[].class }).newInstance(result.toByteArray());
			} else {
				throw new IOException("Unsupported content type: " + contentType);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public <T> void setOutput(HttpServletRequest request, HttpServletResponse response, T result, Class<T> tClass) {
		try (ServletOutputStream out = response.getOutputStream()) {
			if (result == null) {
				return;
			}
			String acceptedHeader = request.getHeader(HttpHeaders.ACCEPT);
			String[] contentTypes = acceptedHeader != null ? acceptedHeader.split(",") : new String[] { null };
			for (String contentType : contentTypes) {
				if (contentType == null || MIME_TYPE_JSON.isCompatible(contentType)) {
					response.setContentType(MIME_TYPE_JSON.toString());
					new ObjectMapper().writeValue(out, result);
					break;
				} else if (MIME_TYPE_XML.isCompatible(contentType)) {
					// MIME_XML
					response.setContentType(MIME_TYPE_XML.toString());
					JAXBContext.newInstance(tClass).createMarshaller().marshal(result, out);
					break;
				}
			}
		} catch (Exception e) {
			// ignore client aborts
		}
	}

}
