package ui.servlets;

import java.util.Collections;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;

import ui.entities.config.Configuration;
import ui.entities.config.EmulationSection;
import ui.entities.config.service.ConfigService;
import ui.entities.config.service.ConfigService.ConfigurationType;

public class JSIDPlay2Server {

	private static final String CONTEXT_ROOT = "/jsidplay2service/JSIDPlay2REST";

	protected static final String MIME_TYPE_JSON = "application/json;charset=UTF-8";
	protected static final String MIME_TYPE_OCTET_STREAM = "application/octet-stream";
	protected static final String MIME_TYPE_MPEG = "audio/mpeg";
	protected static final String MIME_TYPE_SID = "audio/prs.sid";
	protected static final String MIME_TYPE_BIN = "bin";

	private Server server;

	public void start(Configuration configuration) throws Exception {
		final EmulationSection emulationSection = configuration.getEmulationSection();

		server = new Server(emulationSection.getAppServerPort());

		LoginService loginService = new HashLoginService("JSIDPlay2",
				JSIDPlay2Server.class.getResource("/realm.properties").getFile());
		server.addBean(loginService);

		ConstraintSecurityHandler security = new ConstraintSecurityHandler();
		server.setHandler(security);

		Constraint constraint = new Constraint();
		constraint.setName("auth");
		constraint.setAuthenticate(true);
		constraint.setRoles(new String[] { "user", "admin" });

		ConstraintMapping mapping = new ConstraintMapping();
		mapping.setPathSpec("/*");
		mapping.setConstraint(constraint);

		security.setConstraintMappings(Collections.singletonList(mapping));
		security.setAuthenticator(new BasicAuthenticator());
		security.setLoginService(loginService);

		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");

		context.addServlet(new ServletHolder(new FiltersServlet(configuration)),
				CONTEXT_ROOT + FiltersServlet.SERVLET_PATH);
		context.addServlet(new ServletHolder(new DirectoryServlet(configuration)),
				CONTEXT_ROOT + DirectoryServlet.SERVLET_PATH + "/*");
		context.addServlet(new ServletHolder(new TuneInfoServlet(configuration)),
				CONTEXT_ROOT + TuneInfoServlet.SERVLET_PATH + "/*");
		context.addServlet(new ServletHolder(new PhotoServlet(configuration)),
				CONTEXT_ROOT + PhotoServlet.SERVLET_PATH + "/*");
		context.addServlet(new ServletHolder(new ConvertServlet(configuration)),
				CONTEXT_ROOT + ConvertServlet.SERVLET_PATH + "/*");
		context.addServlet(new ServletHolder(new DownloadServlet(configuration)),
				CONTEXT_ROOT + DownloadServlet.SERVLET_PATH + "/*");

		security.setHandler(context);
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
		new JSIDPlay2Server().start(new ConfigService(ConfigurationType.XML).load());
	}
}
