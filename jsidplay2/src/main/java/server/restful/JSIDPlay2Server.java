package server.restful;

import static server.restful.servlets.ConvertServlet.SERVLET_PATH_CONVERT;
import static server.restful.servlets.DirectoryServlet.SERVLET_PATH_DIRECTORY;
import static server.restful.servlets.DownloadServlet.SERVLET_PATH_DOWNLOAD;
import static server.restful.servlets.FavoritesServlet.SERVLET_PATH_FAVORITES;
import static server.restful.servlets.FiltersServlet.SERVLET_PATH_FILTERS;
import static server.restful.servlets.PhotoServlet.SERVLET_PATH_PHOTO;
import static server.restful.servlets.StartPageServlet.SERVLET_PATH_STARTPAGE;
import static server.restful.servlets.TuneInfoServlet.SERVLET_PATH_TUNE_INFO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;

import libsidplay.sidtune.SidTuneError;
import libsidutils.DebugUtil;
import server.restful.servlets.ConvertServlet;
import server.restful.servlets.DirectoryServlet;
import server.restful.servlets.DownloadServlet;
import server.restful.servlets.FavoritesServlet;
import server.restful.servlets.FiltersServlet;
import server.restful.servlets.PhotoServlet;
import server.restful.servlets.StartPageServlet;
import server.restful.servlets.TuneInfoServlet;
import sidplay.Player;
import ui.entities.config.Configuration;
import ui.entities.config.service.ConfigService;
import ui.entities.config.service.ConfigService.ConfigurationType;

public class JSIDPlay2Server {

	static {
		DebugUtil.init();
	}

	/**
	 * Context root of all servlets
	 */
	private static final String CONTEXT_ROOT = "/jsidplay2service/JSIDPlay2REST";

	private static final String SERVLET_PATH_PLAYER = "/player";

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

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	private static JSIDPlay2Server instance;

	private Tomcat tomcat;

	@ParametersDelegate
	private Configuration configuration;

	private Properties directoryProperties = new Properties();

	private JSIDPlay2Server(Configuration configuration) {
		this.configuration = configuration;
		try (InputStream is = new FileInputStream(getDirectoryConfigPath())) {
			directoryProperties.load(is);
		} catch (IOException e) {
			// ignore non-existing properties
		}
		Player.initializeTmpDir(configuration);
		extractWebappResources();
	}

	public static JSIDPlay2Server getInstance(Configuration configuration) {
		if (instance == null) {
			instance = new JSIDPlay2Server(configuration);
		}
		return instance;
	}

	public synchronized void start() throws Exception {
		if (tomcat == null) {
			tomcat = createServer();
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
				tomcat.stop();
				tomcat.getServer().await();
				tomcat.getServer().destroy();
			} finally {
				tomcat = null;
			}
		}
	}

	private void extractWebappResources() {
		for (String filename : Arrays.asList("favorites.vue")) {
			File playerDir = new File(configuration.getSidplay2Section().getTmpDir(), "player");
			playerDir.mkdir();
			Path target = new File(playerDir, filename).toPath();
			try (InputStream source = JSIDPlay2Server.class.getResourceAsStream("/server/restful/webapp/" + filename)) {
				Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private Tomcat createServer() throws IOException {
		Tomcat tomcat = new Tomcat();

		setConnectors(tomcat);

		Context context = createContext(tomcat);

		MemoryRealm realm = new MemoryRealm();
		realm.setPathname(getRealmConfigPath().toString());
		tomcat.getEngine().setRealm(realm);
		
		createAuthorizationConstraints(context);

		addServlets(context);

		return tomcat;
	}

	private Context createContext(Tomcat tomcat) {
		// context root is our .jsidplay2 directory!
		Context context = tomcat.addWebapp(tomcat.getHost(), "", configuration.getSidplay2Section().getTmpDir());
		context.addSecurityRole(ROLE_USER);
		context.addSecurityRole(ROLE_ADMIN);
		StandardJarScanner jarScanner = (StandardJarScanner) context.getJarScanner();
		StandardJarScanFilter jarScanFilter = (StandardJarScanFilter) jarScanner.getJarScanFilter();
		jarScanner.setScanManifest(false);
		jarScanFilter.setTldSkip("*");
		return context;
	}

	private void setConnectors(Tomcat tomcat) {
		switch (configuration.getEmulationSection().getAppServerConnectors()) {
		case HTTP_HTTPS: {
			tomcat.setConnector(createHttpConnector());
			tomcat.setConnector(createHttpsConnector());
			break;
		}
		case HTTPS: {
			tomcat.setConnector(createHttpsConnector());
			break;
		}
		case HTTP:
		default: {
			tomcat.setConnector(createHttpConnector());
			break;
		}
		}
	}

	private Connector createHttpConnector() {
		Connector httpConnector = new Connector();
		httpConnector.setURIEncoding("UTF-8");
		httpConnector.setPort(configuration.getEmulationSection().getAppServerPort());
		return httpConnector;
	}

	private Connector createHttpsConnector() {
		Connector httpsConnector = new Connector();
		httpsConnector.setSecure(true);
		httpsConnector.setScheme("https");
		httpsConnector.setAttribute("sslProtocol", "TLS");
		httpsConnector.setAttribute("SSLEnabled", true);
		httpsConnector.setAttribute("keystoreFile", configuration.getEmulationSection().getAppServerKeystoreFile());
		httpsConnector.setAttribute("keystorePass", configuration.getEmulationSection().getAppServerKeystorePassword());
		httpsConnector.setAttribute("keyAlias", configuration.getEmulationSection().getAppServerKeyAlias());
		httpsConnector.setAttribute("keyPass", configuration.getEmulationSection().getAppServerKeyPassword());
		httpsConnector.setURIEncoding("UTF-8");
		httpsConnector.setPort(configuration.getEmulationSection().getAppServerSecurePort());
		return httpsConnector;
	}

	private void createAuthorizationConstraints(Context context) {
		LoginConfig loginConfig = new LoginConfig();
		loginConfig.setAuthMethod(HttpServletRequest.BASIC_AUTH);
		context.setLoginConfig(loginConfig);

		SecurityConstraint constraint = new SecurityConstraint();
		constraint.addAuthRole(ROLE_ADMIN);
		constraint.addAuthRole(ROLE_USER);
		constraint.setAuthConstraint(true);

		SecurityCollection collection = new SecurityCollection();
		collection.addPattern(CONTEXT_ROOT + "/*");
		constraint.addCollection(collection);
		context.addConstraint(constraint);
	}

	private void addServlets(Context context) {
		Tomcat.addServlet(context, "startPageServlet", new StartPageServlet(configuration, directoryProperties))
				.addMapping(SERVLET_PATH_STARTPAGE);
		Tomcat.addServlet(context, "filtersServlet", new FiltersServlet(configuration, directoryProperties))
				.addMapping(CONTEXT_ROOT + SERVLET_PATH_FILTERS);
		Tomcat.addServlet(context, "directoryServlet", new DirectoryServlet(configuration, directoryProperties))
				.addMapping(CONTEXT_ROOT + SERVLET_PATH_DIRECTORY + "/*");
		Tomcat.addServlet(context, "tuneInfoServlet", new TuneInfoServlet(configuration, directoryProperties))
				.addMapping(CONTEXT_ROOT + SERVLET_PATH_TUNE_INFO + "/*");
		Tomcat.addServlet(context, "photoServlet", new PhotoServlet(configuration, directoryProperties))
				.addMapping(CONTEXT_ROOT + SERVLET_PATH_PHOTO + "/*");
		Tomcat.addServlet(context, "convertServlet", new ConvertServlet(configuration, directoryProperties))
				.addMapping(CONTEXT_ROOT + SERVLET_PATH_CONVERT + "/*");
		Tomcat.addServlet(context, "downloadServlet", new DownloadServlet(configuration, directoryProperties))
				.addMapping(CONTEXT_ROOT + SERVLET_PATH_DOWNLOAD + "/*");
		Tomcat.addServlet(context, "favoritesServlet", new FavoritesServlet(configuration, directoryProperties))
				.addMapping(CONTEXT_ROOT + SERVLET_PATH_FAVORITES);
		Tomcat.addServlet(context, "defaultServlet", new DefaultServlet()).addMapping(SERVLET_PATH_PLAYER + "/*");
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
