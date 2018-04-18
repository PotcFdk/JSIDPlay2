package ui.servlets;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

public class JSIDPlay2Server {

	private static final int PORT = 8080;
	
	private static final String CONTEXT_ROOT = "/jsidplay2service/JSIDPlay2REST";

	protected static final String ROOT_DIR = "/home/ken/Downloads/C64Music";
	protected static final String HVSC_ROOT = ROOT_DIR + "/C64Music";

	protected static final String MIME_TYPE_JSON= "application/json;charset=UTF-8";
	protected static final String MIME_TYPE_OCTET_STREAM= "application/octet-stream";
	protected static final String MIME_TYPE_MPEG= "audio/mpeg";
	protected static final String MIME_TYPE_SID= "audio/prs.sid";
	protected static final String MIME_TYPE_BIN= "bin";

	private Server server;

	public void start() throws Exception {
		server = new Server(PORT);
		ServletHandler handler = new ServletHandler();
		handler.addServletWithMapping(FiltersServlet.class, CONTEXT_ROOT + FiltersServlet.SERVLET_PATH);
		handler.addServletWithMapping(DirectoryServlet.class, CONTEXT_ROOT + DirectoryServlet.SERVLET_PATH + "/*");
		handler.addServletWithMapping(TuneInfoServlet.class, CONTEXT_ROOT + TuneInfoServlet.SERVLET_PATH + "/*");
		handler.addServletWithMapping(PhotoServlet.class, CONTEXT_ROOT + PhotoServlet.SERVLET_PATH + "/*");
		handler.addServletWithMapping(ConvertServlet.class, CONTEXT_ROOT + ConvertServlet.SERVLET_PATH + "/*");
		handler.addServletWithMapping(DownloadServlet.class, CONTEXT_ROOT + DownloadServlet.SERVLET_PATH + "/*");
		server.setHandler(handler);
		server.start();
	}

	public void stop() throws Exception {
		server.stop();
		server.join();
	}
}
