package server.restful.common;

import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_XML;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.ws.rs.core.HttpHeaders;
import javax.xml.bind.JAXBContext;

import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.schlichtherle.truezip.file.TFile;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public abstract class JSIDPlay2Servlet extends HttpServlet {

	protected static final String C64_MUSIC = "/C64Music";
	protected static final String CGSC = "/CGSC";

	protected Configuration configuration;

	protected Properties directoryProperties;

	protected JSIDPlay2Servlet(Configuration configuration, Properties directoryProperties) {
		this.configuration = configuration;
		this.directoryProperties = directoryProperties;
	}

	public abstract String getServletPath();

	protected void doGet(HttpServletRequest request) {
		log(request.getMethod() + " " + request.getRequestURI() + " from " + request.getRemoteAddr()
				+ (request.getQueryString() != null ? "?" + request.getQueryString() : ""));
	}

	protected void doPost(HttpServletRequest request) {
		log(request.getMethod() + " " + request.getRequestURI() + " from " + request.getRemoteAddr());
	}

	protected void doPut(HttpServletRequest request) {
		log(request.getMethod() + " " + request.getRequestURI() + " from " + request.getRemoteAddr());
	}

	@SuppressWarnings("unchecked")
	protected <T> T getInput(HttpServletRequest request, Class<T> tClass) throws IOException {
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
			throw new IOException(e);
		}
	}

	protected <T> void setOutput(HttpServletRequest request, HttpServletResponse response, T result, Class<T> tClass) {
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
					response.setContentType(MIME_TYPE_XML.toString());
					JAXBContext.newInstance(tClass).createMarshaller().marshal(result, out);
					break;
				}
			}
		} catch (Exception e) {
			// ignore client aborts
		}
	}

	protected File getAbsoluteFile(String path, boolean adminRole) throws FileNotFoundException {
		if (path.startsWith(C64_MUSIC)) {
			File rootFile = configuration.getSidplay2Section().getHvsc();
			return PathUtils.getFile(path.substring(C64_MUSIC.length()), rootFile, null);
		} else if (path.startsWith(CGSC)) {
			File rootFile = configuration.getSidplay2Section().getCgsc();
			return PathUtils.getFile(path.substring(CGSC.length()), null, rootFile);
		}
		for (String directoryLogicalName : directoryProperties.stringPropertyNames()) {
			String[] splitted = directoryProperties.getProperty(directoryLogicalName).split(",");
			String directoryValue = splitted.length > 0 ? splitted[0] : null;
			boolean needToBeAdmin = splitted.length > 1 ? Boolean.parseBoolean(splitted[1]) : false;
			if ((!needToBeAdmin || adminRole) && path.startsWith(directoryLogicalName) && directoryValue != null) {
				return PathUtils.getFile(directoryValue + path.substring(directoryLogicalName.length()),
						new TFile(directoryValue), null);
			}
		}
		throw new FileNotFoundException(path);
	}

}
