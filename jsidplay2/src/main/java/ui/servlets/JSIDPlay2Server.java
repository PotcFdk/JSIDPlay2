package ui.servlets;

import static ui.servlets.StartPageServlet.SERVLET_PATH_STARTPAGE;
import static ui.servlets.ConvertServlet.SERVLET_PATH_CONVERT;
import static ui.servlets.DirectoryServlet.SERVLET_PATH_DIRECTORY;
import static ui.servlets.DownloadServlet.SERVLET_PATH_DOWNLOAD;
import static ui.servlets.FiltersServlet.SERVLET_PATH_FILTERS;
import static ui.servlets.PhotoServlet.SERVLET_PATH_PHOTO;
import static ui.servlets.TuneInfoServlet.SERVLET_PATH_TUNE_INFO;

import java.util.Collections;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;

import ui.entities.config.Configuration;
import ui.entities.config.service.ConfigService;
import ui.entities.config.service.ConfigService.ConfigurationType;

public class JSIDPlay2Server {

	private static final String CONTEXT_ROOT = "/jsidplay2service/JSIDPlay2REST";

	protected static final String MIME_TYPE_JPG = "image/jpeg";
	protected static final String MIME_TYPE_MPEG = "audio/mpeg";
	protected static final String MIME_TYPE_SID = "audio/prs.sid";
	protected static final String MIME_TYPE_BIN = "bin";

	private Configuration configuration;

	private HashLoginService loginService;

	private ConstraintSecurityHandler security;

	private Server server;

	public JSIDPlay2Server(Configuration configuration) {
		this.configuration = configuration;

		StartPageServlet startPageServlet = new StartPageServlet(configuration);
		FiltersServlet filtersServlet = new FiltersServlet(configuration);
		DirectoryServlet directoryServlet = new DirectoryServlet(configuration);
		TuneInfoServlet tuneInfoServlet = new TuneInfoServlet(configuration);
		PhotoServlet photoServlet = new PhotoServlet(configuration);
		ConvertServlet convertServlet = new ConvertServlet(configuration);
		DownloadServlet downloadServlet = new DownloadServlet(configuration);

		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		context.addServlet(new ServletHolder(startPageServlet), SERVLET_PATH_STARTPAGE);
		context.addServlet(new ServletHolder(filtersServlet), CONTEXT_ROOT + SERVLET_PATH_FILTERS);
		context.addServlet(new ServletHolder(directoryServlet), CONTEXT_ROOT + SERVLET_PATH_DIRECTORY + "/*");
		context.addServlet(new ServletHolder(tuneInfoServlet), CONTEXT_ROOT + SERVLET_PATH_TUNE_INFO + "/*");
		context.addServlet(new ServletHolder(photoServlet), CONTEXT_ROOT + SERVLET_PATH_PHOTO + "/*");
		context.addServlet(new ServletHolder(convertServlet), CONTEXT_ROOT + SERVLET_PATH_CONVERT + "/*");
		context.addServlet(new ServletHolder(downloadServlet), CONTEXT_ROOT + SERVLET_PATH_DOWNLOAD + "/*");

		Constraint constraint = new Constraint();
		constraint.setName("auth");
		constraint.setAuthenticate(true);
		constraint.setRoles(new String[] { "user", "admin" });

		ConstraintMapping mapping = new ConstraintMapping();
		mapping.setPathSpec(CONTEXT_ROOT + "/*");
		mapping.setConstraint(constraint);

		loginService = new HashLoginService("JSIDPlay2",
				JSIDPlay2Server.class.getResource("/realm.properties").toExternalForm());

		security = new ConstraintSecurityHandler();
		security.setConstraintMappings(Collections.singletonList(mapping));
		security.setAuthenticator(new BasicAuthenticator());
		security.setLoginService(loginService);
		security.setHandler(context);
	}

	public void start() throws Exception {
		server = new Server(configuration.getEmulationSection().getAppServerPort());
		server.addBean(loginService);
		server.setHandler(security);
		server.start();
	}

	public void stop() throws Exception {
		if (server != null) {
			server.stop();
			server.join();
		}
	}

	public static void main(String[] args) throws Exception {
		new JSIDPlay2Server(new ConfigService(ConfigurationType.XML).load()).start();
	}
}
