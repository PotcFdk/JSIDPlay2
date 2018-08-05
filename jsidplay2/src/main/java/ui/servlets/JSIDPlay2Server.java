package ui.servlets;

import static ui.servlets.ConvertServlet.SERVLET_PATH_CONVERT;
import static ui.servlets.DirectoryServlet.SERVLET_PATH_DIRECTORY;
import static ui.servlets.DownloadServlet.SERVLET_PATH_DOWNLOAD;
import static ui.servlets.FavoritesServlet.SERVLET_PATH_FAVORITES;
import static ui.servlets.FiltersServlet.SERVLET_PATH_FILTERS;
import static ui.servlets.PhotoServlet.SERVLET_PATH_PHOTO;
import static ui.servlets.StartPageServlet.SERVLET_PATH_STARTPAGE;
import static ui.servlets.TuneInfoServlet.SERVLET_PATH_TUNE_INFO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;

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

	public static final String ROLE_ADMIN = "admin";

	/**
	 * Filename of the configuration file containing additional directories of admin
	 * role. e.g. "/MP3=/media/nas1/mp3" (top-level logical directory name=real
	 * directory name)
	 */
	public static final String CONFIG_FILE = "directoryServlet";

	private static JSIDPlay2Server instance;

	private Server server;

	private Configuration configuration;

	private Properties directoryProperties = new Properties();

	private JSIDPlay2Server() {
		try (InputStream is = new FileInputStream(getConfigPath())) {
			directoryProperties.load(is);
		} catch (IOException e) {
			// ignore non-existing properties
		}
	}

	public static JSIDPlay2Server getInstance() {
		if (instance == null) {
			instance = new JSIDPlay2Server();
		}
		return instance;
	}

	public void setConfiguration(Configuration configuration) {
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

		Server server = new Server();
		server.setConnectors(getConnectors(server));
		server.addBean(loginService);
		server.setHandler(securityHandler);
		return server;
	}

	private Connector[] getConnectors(Server server) {
		switch (configuration.getEmulationSection().getAppServerConnectors()) {
		case HTTP_HTTPS: {
			ServerConnector httpConnector = new ServerConnector(server);
			httpConnector.setPort(configuration.getEmulationSection().getAppServerPort());

			ServerConnector httpsSslConnector = getHttpsConnector(server);
			httpsSslConnector.setPort(configuration.getEmulationSection().getAppServerSecurePort());
			return new Connector[] { httpConnector, httpsSslConnector };
		}
		case HTTPS: {
			ServerConnector httpsSslConnector = getHttpsConnector(server);
			httpsSslConnector.setPort(configuration.getEmulationSection().getAppServerSecurePort());
			return new Connector[] { httpsSslConnector };
		}
		case HTTP_ONLY:
		default: {
			ServerConnector httpConnector = new ServerConnector(server);
			httpConnector.setPort(configuration.getEmulationSection().getAppServerPort());
			return new Connector[] { httpConnector };
		}
		}
	}

	private ServerConnector getHttpsConnector(Server server) {
		HttpConfiguration https = new HttpConfiguration();
		https.addCustomizer(new SecureRequestCustomizer());
		SslContextFactory sslContextFactory = getSslContextFactory();

		ServerConnector httpsSslConnector = new ServerConnector(server,
				new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
				new HttpConnectionFactory(https));
		return httpsSslConnector;
	}

	private SslContextFactory getSslContextFactory() {
		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(configuration.getEmulationSection().getAppServerKeystoreFile());
		sslContextFactory.setKeyStorePassword(configuration.getEmulationSection().getAppServerKeyStorePassword());
		sslContextFactory.setKeyManagerPassword(configuration.getEmulationSection().getAppServerKeyManagerPassword());
		return sslContextFactory;
	}

	private ConstraintSecurityHandler createSecurity() {
		ConstraintSecurityHandler security = new ConstraintSecurityHandler();
		security.setConstraintMappings(createConstrainedMappings());
		security.setAuthenticator(new BasicAuthenticator());
		return security;
	}

	private ServletContextHandler createServletContextHandler() {
		HttpServlet startPageServlet = new StartPageServlet(configuration, directoryProperties);
		HttpServlet filtersServlet = new FiltersServlet(configuration, directoryProperties);
		HttpServlet directoryServlet = new DirectoryServlet(configuration, directoryProperties);
		HttpServlet tuneInfoServlet = new TuneInfoServlet(configuration, directoryProperties);
		HttpServlet photoServlet = new PhotoServlet(configuration, directoryProperties);
		HttpServlet convertServlet = new ConvertServlet(configuration, directoryProperties);
		HttpServlet downloadServlet = new DownloadServlet(configuration, directoryProperties);
		HttpServlet favoritesServlet = new FavoritesServlet(configuration, directoryProperties);

		ServletContextHandler contextHandler = new ServletContextHandler();
		contextHandler.setContextPath("/");
		contextHandler.addServlet(new ServletHolder(startPageServlet), SERVLET_PATH_STARTPAGE);
		contextHandler.addServlet(new ServletHolder(filtersServlet), CONTEXT_ROOT + SERVLET_PATH_FILTERS);
		contextHandler.addServlet(new ServletHolder(directoryServlet), CONTEXT_ROOT + SERVLET_PATH_DIRECTORY + "/*");
		contextHandler.addServlet(new ServletHolder(tuneInfoServlet), CONTEXT_ROOT + SERVLET_PATH_TUNE_INFO + "/*");
		contextHandler.addServlet(new ServletHolder(photoServlet), CONTEXT_ROOT + SERVLET_PATH_PHOTO + "/*");
		contextHandler.addServlet(new ServletHolder(convertServlet), CONTEXT_ROOT + SERVLET_PATH_CONVERT + "/*");
		contextHandler.addServlet(new ServletHolder(downloadServlet), CONTEXT_ROOT + SERVLET_PATH_DOWNLOAD + "/*");
		contextHandler.addServlet(new ServletHolder(favoritesServlet), CONTEXT_ROOT + SERVLET_PATH_FAVORITES);
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
		JSIDPlay2Server jsidplay2Server = JSIDPlay2Server.getInstance();
		jsidplay2Server.setConfiguration(new ConfigService(ConfigurationType.XML).load());
		jsidplay2Server.start();
	}

	/**
	 * Search for optional configuration containing additional directories. Search
	 * in CWD and in the HOME folder.
	 * 
	 * @return configuration file
	 */
	private File getConfigPath() {
		for (final String s : new String[] { System.getProperty("user.dir"), System.getProperty("user.home"), }) {
			File configPlace = new File(s, CONFIG_FILE + ".properties");
			if (configPlace.exists()) {
				return configPlace;
			}
		}
		// default directory
		return new File(System.getProperty("user.home"), CONFIG_FILE + ".properties");
	}

}
