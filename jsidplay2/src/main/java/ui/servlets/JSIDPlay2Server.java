package ui.servlets;

import static ui.servlets.ConvertServlet.SERVLET_PATH_CONVERT;
import static ui.servlets.DirectoryServlet.SERVLET_PATH_DIRECTORY;
import static ui.servlets.DownloadServlet.SERVLET_PATH_DOWNLOAD;
import static ui.servlets.FiltersServlet.SERVLET_PATH_FILTERS;
import static ui.servlets.PhotoServlet.SERVLET_PATH_PHOTO;
import static ui.servlets.StartPageServlet.SERVLET_PATH_STARTPAGE;
import static ui.servlets.TuneInfoServlet.SERVLET_PATH_TUNE_INFO;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;

import libsidutils.DebugUtil;
import ui.JSidPlay2;
import ui.entities.config.Configuration;
import ui.entities.config.service.ConfigService;
import ui.entities.config.service.ConfigService.ConfigurationType;

public class JSIDPlay2Server {

	static {
		DebugUtil.init();
	}

	private static final String CONTEXT_ROOT = "/jsidplay2service/JSIDPlay2REST";

	private static final URL SERVER_SECURITY_CONFIG = JSIDPlay2Server.class.getResource("/realm.properties");

	private static final String[] ROLES = new String[] { "user", "admin" };

	private Server server;

	private final Configuration configuration;

	public JSIDPlay2Server(Configuration configuration) {
		this.configuration = configuration;
	}

	public void start() throws Exception {
		if (server == null || server.isStopped()) {
			server = createServer();
			server.start();
		}
	}

	public void stop() throws Exception {
		if (server != null && server.isRunning()) {
			server.stop();
			server.join();
		}
	}

	private Server createServer() {
		String config = SERVER_SECURITY_CONFIG.toExternalForm();
		HashLoginService loginService = new HashLoginService(JSidPlay2.class.getSimpleName(), config);

		ConstraintSecurityHandler securityHandler = createSecurity();
		securityHandler.setLoginService(loginService);
		securityHandler.setHandler(createServletContextHandler());

		Server server = new Server(configuration.getEmulationSection().getAppServerPort());
		server.addBean(loginService);
		server.setHandler(securityHandler);
		return server;
	}

	private ConstraintSecurityHandler createSecurity() {
		ConstraintSecurityHandler security = new ConstraintSecurityHandler();
		security.setConstraintMappings(createConstrainedMappings());
		security.setAuthenticator(new BasicAuthenticator());
		return security;
	}

	private ServletContextHandler createServletContextHandler() {
		HttpServlet startPageServlet = new StartPageServlet(configuration);
		HttpServlet filtersServlet = new FiltersServlet(configuration);
		HttpServlet directoryServlet = new DirectoryServlet(configuration);
		HttpServlet tuneInfoServlet = new TuneInfoServlet(configuration);
		HttpServlet photoServlet = new PhotoServlet(configuration);
		HttpServlet convertServlet = new ConvertServlet(configuration);
		HttpServlet downloadServlet = new DownloadServlet(configuration);

		ServletContextHandler contextHandler = new ServletContextHandler();
		contextHandler.setContextPath("/");
		contextHandler.addServlet(new ServletHolder(startPageServlet), SERVLET_PATH_STARTPAGE);
		contextHandler.addServlet(new ServletHolder(filtersServlet), CONTEXT_ROOT + SERVLET_PATH_FILTERS);
		contextHandler.addServlet(new ServletHolder(directoryServlet), CONTEXT_ROOT + SERVLET_PATH_DIRECTORY + "/*");
		contextHandler.addServlet(new ServletHolder(tuneInfoServlet), CONTEXT_ROOT + SERVLET_PATH_TUNE_INFO + "/*");
		contextHandler.addServlet(new ServletHolder(photoServlet), CONTEXT_ROOT + SERVLET_PATH_PHOTO + "/*");
		contextHandler.addServlet(new ServletHolder(convertServlet), CONTEXT_ROOT + SERVLET_PATH_CONVERT + "/*");
		contextHandler.addServlet(new ServletHolder(downloadServlet), CONTEXT_ROOT + SERVLET_PATH_DOWNLOAD + "/*");
		return contextHandler;
	}

	private List<ConstraintMapping> createConstrainedMappings() {
		Constraint constraint = new Constraint();
		constraint.setName("auth");
		constraint.setAuthenticate(true);
		constraint.setRoles(ROLES);

		ConstraintMapping constrainedMapping = new ConstraintMapping();
		constrainedMapping.setPathSpec(CONTEXT_ROOT + "/*");
		constrainedMapping.setConstraint(constraint);
		return Collections.singletonList(constrainedMapping);
	}

	public static void main(String[] args) throws Exception {
		new JSIDPlay2Server(new ConfigService(ConfigurationType.XML).load()).start();
	}
}
