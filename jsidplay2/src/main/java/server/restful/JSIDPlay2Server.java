package server.restful;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static javax.servlet.http.HttpServletRequest.BASIC_AUTH;
import static org.apache.catalina.startup.Tomcat.addServlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.util.List;
import java.util.Properties;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.net.Constants;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;

import libsidplay.sidtune.SidTuneError;
import libsidutils.DebugUtil;
import server.restful.common.Connectors;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.servlets.ConvertServlet;
import server.restful.servlets.DirectoryServlet;
import server.restful.servlets.DownloadServlet;
import server.restful.servlets.FavoritesServlet;
import server.restful.servlets.FiltersServlet;
import server.restful.servlets.PhotoServlet;
import server.restful.servlets.StartPageServlet;
import server.restful.servlets.StaticServlet;
import server.restful.servlets.TuneInfoServlet;
import sidplay.Player;
import ui.entities.config.Configuration;
import ui.entities.config.EmulationSection;
import ui.entities.config.SidPlay2Section;
import ui.entities.config.service.ConfigService;
import ui.entities.config.service.ConfigService.ConfigurationType;

public class JSIDPlay2Server {

	static {
		DebugUtil.init();
	}

	/**
	 * Context root of start page
	 */
	public static final String CONTEXT_ROOT = "";

	/**
	 * Context root of all servlets
	 */
	public static final String CONTEXT_ROOT_SERVLET = "/jsidplay2service/JSIDPlay2REST";

	/**
	 * Context root of static pages
	 */
	public static final String CONTEXT_ROOT_STATIC = "/static";

	/**
	 * User role
	 */
	private static final String ROLE_USER = "user";

	/**
	 * Admin role
	 */
	public static final String ROLE_ADMIN = "admin";

	/**
	 * Filename of the configuration file containing additional directories of
	 * {@link DirectoryServlet}<BR>
	 * e.g. "/MP3=/media/nas1/mp3,true" (top-level logical directory name=real
	 * directory name, admin role required)
	 */
	public static final String DIRECTORIES_CONFIG_FILE = "directoryServlet.properties";

	/**
	 * Filename of the configuration file containing username, password and role.
	 * For an example please refer to the internal file tomcat-users.xml
	 */
	public static final String REALM_CONFIG_FILE = "tomcat-users.xml";

	/**
	 * Configuration of usernames, passwords and roles
	 */
	private static final URL INTERNAL_REALM_CONFIG = JSIDPlay2Server.class.getResource("/" + REALM_CONFIG_FILE);

	/**
	 * Our servlets to serve
	 */
	private static final List<Class<? extends JSIDPlay2Servlet>> SERVLETS = asList(FiltersServlet.class,
			DirectoryServlet.class, TuneInfoServlet.class, PhotoServlet.class, ConvertServlet.class,
			DownloadServlet.class, FavoritesServlet.class, StaticServlet.class, StartPageServlet.class);

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@ParametersDelegate
	private Configuration configuration;

	private Tomcat tomcat;

	private Properties directoryProperties = new Properties();

	private static JSIDPlay2Server instance;

	public static JSIDPlay2Server getInstance(Configuration configuration) {
		if (instance == null) {
			instance = new JSIDPlay2Server(configuration);
		}
		return instance;
	}

	private JSIDPlay2Server(Configuration configuration) {
		this.configuration = configuration;
		try (InputStream is = new FileInputStream(getDirectoryConfigPath())) {
			directoryProperties.load(is);
		} catch (IOException e) {
			// ignore non-existing properties
		}
		Player.initializeTmpDir(configuration);
	}

	public synchronized void start() throws Exception {
		if (tomcat == null) {
			tomcat = createTomcat();
			tomcat.start();
		}
	}

	public synchronized void stop() throws Exception {
		if (tomcat != null && tomcat.getServer().getState() != LifecycleState.STOPPING_PREP
				&& tomcat.getServer().getState() != LifecycleState.STOPPING
				&& tomcat.getServer().getState() != LifecycleState.STOPPED
				&& tomcat.getServer().getState() != LifecycleState.DESTROYING
				&& tomcat.getServer().getState() != LifecycleState.DESTROYED) {
			try {
				try {
					tomcat.stop();
					tomcat.getServer().await();
				} catch (Throwable e) {
					// workaround java11 problem
				}
				tomcat.getServer().destroy();
			} finally {
				tomcat = null;
			}
		}
	}

	/**
	 * Search for optional configuration containing additional directories. Search
	 * in CWD and in the HOME folder.
	 * 
	 * @return configuration file
	 */
	private File getDirectoryConfigPath() {
		for (final String dir : new String[] { System.getProperty("user.dir"), System.getProperty("user.home"), }) {
			File configPlace = new File(dir, DIRECTORIES_CONFIG_FILE);
			if (configPlace.exists()) {
				return configPlace;
			}
		}
		// default directory
		return new File(System.getProperty("user.home"), DIRECTORIES_CONFIG_FILE);
	}

	private Tomcat createTomcat() throws Exception {
		SidPlay2Section sidplay2Section = configuration.getSidplay2Section();
		EmulationSection emulationSection = configuration.getEmulationSection();

		Tomcat tomcat = new Tomcat();
		tomcat.setAddDefaultWebXmlToWebapp(false);
		tomcat.setBaseDir(sidplay2Section.getTmpDir());

		setRealm(tomcat);
		setConnectors(tomcat, emulationSection);

		Context context = addWebApp(tomcat, sidplay2Section);

		addSecurityConstraints(context);
		addServlets(context);

		return tomcat;
	}

	private void setRealm(Tomcat tomcat) {
		MemoryRealm realm = new MemoryRealm();
		realm.setPathname(getRealmConfigPath().toExternalForm());
		tomcat.getEngine().setRealm(realm);
	}

	/**
	 * Search for user, password and role configuration file.<BR>
	 * <B>Note:</B>If no configuration file is found internal configuration is used
	 * 
	 * @return user, password and role configuration file
	 */
	private URL getRealmConfigPath() {
		for (final String dir : new String[] { System.getProperty("user.dir"), System.getProperty("user.home"), }) {
			File configPlace = new File(dir, REALM_CONFIG_FILE);
			if (configPlace.exists()) {
				try {
					return configPlace.toURI().toURL();
				} catch (MalformedURLException e) {
					// ignore, use internal config instead!
				}
			}
		}
		// built-in default configuration
		return INTERNAL_REALM_CONFIG;
	}

	private void setConnectors(Tomcat tomcat, EmulationSection emulationSection) {
		switch (emulationSection.getAppServerConnectors()) {
		case HTTP_HTTPS: {
			tomcat.setConnector(createHttpConnector(emulationSection));
			tomcat.setConnector(createHttpsConnector(emulationSection));
			break;
		}
		case HTTPS: {
			tomcat.setConnector(createHttpsConnector(emulationSection));
			break;
		}
		case HTTP:
		default: {
			tomcat.setConnector(createHttpConnector(emulationSection));
			break;
		}
		}
	}

	private Connector createHttpConnector(EmulationSection emulationSection) {
		Connector httpConnector = new Connector(Http11NioProtocol.class.getName());
		httpConnector.setURIEncoding(UTF_8.name());
		httpConnector.setPort(emulationSection.getAppServerPort());
		httpConnector.setScheme(Connectors.HTTP.getPreferredProtocol());
		return httpConnector;
	}

	private Connector createHttpsConnector(EmulationSection emulationSection) {
		Connector httpsConnector = new Connector(Http11NioProtocol.class.getName());
		httpsConnector.setURIEncoding(UTF_8.name());
		httpsConnector.setPort(emulationSection.getAppServerSecurePort());
		httpsConnector.setScheme(Connectors.HTTPS.getPreferredProtocol());

		Http11NioProtocol protocol = (Http11NioProtocol) httpsConnector.getProtocolHandler();
		protocol.setSecure(true);
		protocol.setSSLEnabled(true);
		protocol.setSslProtocol(Constants.SSL_PROTO_TLSv1_2);

		asList(protocol.findSslHostConfigs()).stream().findFirst().ifPresent(sslHostConfig -> {
			SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(sslHostConfig, Type.RSA);
			certificate.setCertificateKeystoreType(KeyStore.getDefaultType());
			certificate.setCertificateKeystoreFile(emulationSection.getAppServerKeystoreFile());
			certificate.setCertificateKeystorePassword(emulationSection.getAppServerKeystorePassword());
			certificate.setCertificateKeyAlias(emulationSection.getAppServerKeyAlias());
			certificate.setCertificateKeyPassword(emulationSection.getAppServerKeyPassword());

			sslHostConfig.addCertificate(certificate);
		});
		return httpsConnector;
	}

	/**
	 * <b>Note:</b> Base directory of the context root is .jsidplay2
	 */
	private Context addWebApp(Tomcat tomcat, SidPlay2Section sidplay2Section) {
		Context context = tomcat.addWebapp(tomcat.getHost(), CONTEXT_ROOT, sidplay2Section.getTmpDir());
		context.addSecurityRole(ROLE_ADMIN);
		context.addSecurityRole(ROLE_USER);
		StandardJarScanner jarScanner = (StandardJarScanner) context.getJarScanner();
		StandardJarScanFilter jarScanFilter = (StandardJarScanFilter) jarScanner.getJarScanFilter();
		jarScanner.setScanManifest(false);
		jarScanFilter.setTldSkip("*");
		return context;
	}

	private void addSecurityConstraints(Context context) {
		context.setLoginConfig(new LoginConfig(BASIC_AUTH, null, null, null));

		SecurityConstraint constraint = new SecurityConstraint();
		constraint.addAuthRole(ROLE_ADMIN);
		constraint.addAuthRole(ROLE_USER);
		constraint.setAuthConstraint(true);

		SecurityCollection collection = new SecurityCollection();
		collection.addPattern(CONTEXT_ROOT_SERVLET + "/*");
		constraint.addCollection(collection);
		context.addConstraint(constraint);
	}

	private void addServlets(Context context) throws Exception {
		for (Class<? extends JSIDPlay2Servlet> servletCls : SERVLETS) {
			JSIDPlay2Servlet servlet = (JSIDPlay2Servlet) servletCls
					.getDeclaredConstructor(Configuration.class, Properties.class)
					.newInstance(configuration, directoryProperties);
			addServlet(context, servletCls.getSimpleName(), servlet).addMapping(servlet.getServletPath() + "/*");
		}
	}

	private static void exit(int rc) {
		try {
			System.out.println("Press <enter> to exit the player!");
			System.in.read();
			System.exit(rc);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	public static void main(String[] args) throws Exception {
		try {
			Configuration configuration = new ConfigService(ConfigurationType.XML).load();
			JSIDPlay2Server jsidplay2Server = getInstance(configuration);
			JCommander commander = JCommander.newBuilder().addObject(jsidplay2Server)
					.programName(JSIDPlay2Server.class.getName()).build();
			commander.parse(args);
			if (jsidplay2Server.help) {
				commander.usage();
				exit(1);
			}
			jsidplay2Server.start();
		} catch (ParameterException | IOException | SidTuneError e) {
			System.err.println(e.getMessage());
			exit(1);
		}
	}

}
