package applet.entities.config.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import sidplay.ini.IniConfig;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IFavoritesSection;
import sidplay.ini.intf.IFilterSection;
import applet.entities.config.DbAudioSection;
import applet.entities.config.DbC1541Section;
import applet.entities.config.DbConfig;
import applet.entities.config.DbConsoleSection;
import applet.entities.config.DbEmulationSection;
import applet.entities.config.DbFavoritesSection;
import applet.entities.config.DbFilterSection;
import applet.entities.config.DbJoystickSection;
import applet.entities.config.DbOnlineSection;
import applet.entities.config.DbPrinterSection;
import applet.entities.config.DbSidPlay2Section;

public class DbConfigService {
	private EntityManager em;

	public DbConfigService(EntityManager em) {
		this.em = em;
	};

	public boolean isExpectedVersion(IConfig config) {
		return config != null
				&& config.getSidplay2().getVersion() == IConfig.REQUIRED_CONFIG_VERSION;
	}

	public void setExpectedVersion(DbConfig config) {
		((DbSidPlay2Section) config.getSidplay2())
				.setVersion(IConfig.REQUIRED_CONFIG_VERSION);
	}

	public void remove(DbConfig config) {
		// remove old configuration from DB
		em.getTransaction().begin();
		em.remove(config);
		em.flush();
		em.clear();
		em.getTransaction().commit();
	}

	public DbConfig get() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<DbConfig> q = cb.createQuery(DbConfig.class);
		Root<DbConfig> h = q.from(DbConfig.class);
		q.select(h);
		List<DbConfig> resultList = em.createQuery(q).setMaxResults(1)
				.getResultList();
		if (resultList.size() != 0) {
			return resultList.get(0);
		}
		return null;
	}

	public IFavoritesSection addFavorite(IConfig config, String title) {
		DbConfig dbConfig = (DbConfig) config;
		DbFavoritesSection toAdd = new DbFavoritesSection();
		toAdd.setDbConfig(dbConfig);
		toAdd.setName(title);
		dbConfig.getFavoritesInternal().add(toAdd);
		em.persist(toAdd);
		flush();
		return toAdd;
	}

	public void removeFavorite(IConfig config, int index) {
		DbConfig dbConfig = (DbConfig) config;
		DbFavoritesSection toRemove = (DbFavoritesSection) dbConfig
				.getFavorites().get(index);
		toRemove.setDbConfig(null);
		dbConfig.getFavorites().remove(index);
		em.remove(toRemove);
		flush();
	}

	public void write(IConfig iConfig) {
		em.getTransaction().begin();
		try {
			em.persist(iConfig);
			em.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
	}

	/**
	 * Import old settings from INI file. Only for migration purposes!
	 * 
	 * @param iniConfig
	 *            INI file to import
	 * @return persistent properties
	 */
	public DbConfig importIniConfig(IniConfig iniConfig) {
		DbConfig dbConfig = new DbConfig();

		DbSidPlay2Section sidplay2 = new DbSidPlay2Section();
		dbConfig.setSidplay2(sidplay2);
		sidplay2.setEnableDatabase(iniConfig.getSidplay2().isEnableDatabase());
		sidplay2.setPlayLength(iniConfig.getSidplay2().getPlayLength());
		sidplay2.setRecordLength(iniConfig.getSidplay2().getRecordLength());
		sidplay2.setHVMEC(iniConfig.getSidplay2().getHVMEC());
		sidplay2.setDemos(iniConfig.getSidplay2().getDemos());
		sidplay2.setMags(iniConfig.getSidplay2().getMags());
		sidplay2.setCgsc(iniConfig.getSidplay2().getCgsc());
		sidplay2.setHvsc(iniConfig.getSidplay2().getHvsc());
		sidplay2.setSingle(iniConfig.getSidplay2().isSingle());
		sidplay2.setEnableProxy(iniConfig.getSidplay2().isEnableProxy());
		sidplay2.setProxyHostname(iniConfig.getSidplay2().getProxyHostname());
		sidplay2.setProxyPort(iniConfig.getSidplay2().getProxyPort());
		sidplay2.setLastDirectory(iniConfig.getSidplay2().getLastDirectory());
		sidplay2.setTmpDir(iniConfig.getSidplay2().getTmpDir());
		sidplay2.setFrameX(iniConfig.getSidplay2().getFrameX());
		sidplay2.setFrameY(iniConfig.getSidplay2().getFrameY());
		sidplay2.setFrameWidth(iniConfig.getSidplay2().getFrameWidth());
		sidplay2.setFrameHeight(iniConfig.getSidplay2().getFrameHeight());

		DbOnlineSection online = new DbOnlineSection();
		dbConfig.setOnline(online);
		online.setSoasc6581R2(iniConfig.getOnline().getSoasc6581R2());
		online.setSoasc6581R4(iniConfig.getOnline().getSoasc6581R4());
		online.setSoasc8580R5(iniConfig.getOnline().getSoasc8580R5());

		DbC1541Section c1541 = new DbC1541Section();
		dbConfig.setC1541(c1541);
		c1541.setDriveOn(iniConfig.getC1541().isDriveOn());
		c1541.setDriveSoundOn(iniConfig.getC1541().isDriveSoundOn());
		c1541.setParallelCable(iniConfig.getC1541().isParallelCable());
		c1541.setRamExpansion0(iniConfig.getC1541().isRamExpansionEnabled0());
		c1541.setRamExpansion1(iniConfig.getC1541().isRamExpansionEnabled1());
		c1541.setRamExpansion2(iniConfig.getC1541().isRamExpansionEnabled2());
		c1541.setRamExpansion3(iniConfig.getC1541().isRamExpansionEnabled3());
		c1541.setRamExpansion4(iniConfig.getC1541().isRamExpansionEnabled4());
		c1541.setExtendImagePolicy(iniConfig.getC1541().getExtendImagePolicy());
		c1541.setFloppyType(iniConfig.getC1541().getFloppyType());

		DbPrinterSection printer = new DbPrinterSection();
		dbConfig.setPrinter(printer);
		printer.setPrinterOn(iniConfig.getPrinter().isPrinterOn());

		DbJoystickSection joystick = new DbJoystickSection();
		dbConfig.setJoystick(joystick);
		joystick.setDeviceName1(iniConfig.getJoystick().getDeviceName1());
		joystick.setDeviceName2(iniConfig.getJoystick().getDeviceName2());
		joystick.setComponentNameUp1(iniConfig.getJoystick()
				.getComponentNameUp1());
		joystick.setComponentNameUp2(iniConfig.getJoystick()
				.getComponentNameUp2());
		joystick.setComponentNameDown1(iniConfig.getJoystick()
				.getComponentNameDown1());
		joystick.setComponentNameDown2(iniConfig.getJoystick()
				.getComponentNameDown2());
		joystick.setComponentNameLeft1(iniConfig.getJoystick()
				.getComponentNameLeft1());
		joystick.setComponentNameLeft2(iniConfig.getJoystick()
				.getComponentNameLeft2());
		joystick.setComponentNameRight1(iniConfig.getJoystick()
				.getComponentNameRight1());
		joystick.setComponentNameRight2(iniConfig.getJoystick()
				.getComponentNameRight2());
		joystick.setComponentNameBtn1(iniConfig.getJoystick()
				.getComponentNameBtn1());
		joystick.setComponentNameBtn2(iniConfig.getJoystick()
				.getComponentNameBtn2());
		joystick.setComponentValueUp1(iniConfig.getJoystick()
				.getComponentValueUp1());
		joystick.setComponentValueUp2(iniConfig.getJoystick()
				.getComponentValueUp2());
		joystick.setComponentValueDown1(iniConfig.getJoystick()
				.getComponentValueDown1());
		joystick.setComponentValueDown2(iniConfig.getJoystick()
				.getComponentValueDown2());
		joystick.setComponentValueLeft1(iniConfig.getJoystick()
				.getComponentValueLeft1());
		joystick.setComponentValueLeft2(iniConfig.getJoystick()
				.getComponentValueLeft2());
		joystick.setComponentValueRight1(iniConfig.getJoystick()
				.getComponentValueRight1());
		joystick.setComponentValueRight2(iniConfig.getJoystick()
				.getComponentValueRight2());
		joystick.setComponentValueBtn1(iniConfig.getJoystick()
				.getComponentValueBtn1());
		joystick.setComponentValueBtn2(iniConfig.getJoystick()
				.getComponentValueBtn2());

		DbConsoleSection console = new DbConsoleSection();
		dbConfig.setConsole(console);
		console.setBottomLeft(iniConfig.getConsole().getBottomLeft());
		console.setBottomRight(iniConfig.getConsole().getBottomRight());
		console.setHorizontal(iniConfig.getConsole().getHorizontal());
		console.setVertical(iniConfig.getConsole().getVertical());
		console.setJunctionLeft(iniConfig.getConsole().getJunctionLeft());
		console.setJunctionRight(iniConfig.getConsole().getJunctionRight());
		console.setTopLeft(iniConfig.getConsole().getTopLeft());
		console.setTopRight(iniConfig.getConsole().getTopRight());

		DbAudioSection audio = new DbAudioSection();
		dbConfig.setAudio(audio);
		audio.setFrequency(iniConfig.getAudio().getFrequency());
		audio.setSampling(iniConfig.getAudio().getSampling());
		audio.setPlayOriginal(iniConfig.getAudio().isPlayOriginal());
		audio.setMp3File(iniConfig.getAudio().getMp3File());
		audio.setLeftVolume(iniConfig.getAudio().getLeftVolume());
		audio.setRightVolume(iniConfig.getAudio().getRightVolume());

		DbEmulationSection emulation = new DbEmulationSection();
		dbConfig.setEmulation(emulation);
		emulation.setDefaultClockSpeed(iniConfig.getEmulation()
				.getDefaultClockSpeed());
		emulation.setUserClockSpeed(iniConfig.getEmulation()
				.getUserClockSpeed());
		emulation.setDefaultSidModel(iniConfig.getEmulation()
				.getDefaultSidModel());
		emulation.setUserSidModel(iniConfig.getEmulation().getUserSidModel());
		emulation.setHardsid6581(iniConfig.getEmulation().getHardsid6581());
		emulation.setHardsid8580(iniConfig.getEmulation().getHardsid8580());
		emulation.setFilter(iniConfig.getEmulation().isFilter());
		emulation.setFilter6581(iniConfig.getEmulation().getFilter6581());
		emulation.setFilter8580(iniConfig.getEmulation().getFilter8580());
		emulation.setDigiBoosted8580(iniConfig.getEmulation()
				.isDigiBoosted8580());
		emulation.setDualSidBase(iniConfig.getEmulation().getDualSidBase());
		emulation.setForceStereoTune(iniConfig.getEmulation()
				.isForceStereoTune());
		emulation.setStereoSidModel(iniConfig.getEmulation()
				.getStereoSidModel());

		ArrayList<DbFavoritesSection> newFavoritesList = new ArrayList<DbFavoritesSection>();
		for (IFavoritesSection f : iniConfig.getFavorites()) {
			DbFavoritesSection newFavorite = new DbFavoritesSection();
			newFavorite.setDbConfig(dbConfig);
			newFavorite.setName(f.getName());
			if (f.getFilename() != null) {
				try (BufferedReader r = new BufferedReader(
						new InputStreamReader(new FileInputStream(
								f.getFilename()), "ISO-8859-1"))) {
					String line;
					while ((line = r.readLine()) != null) {
						newFavorite.getFavorites().add(line);
					}
					r.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			newFavoritesList.add(newFavorite);
		}
		dbConfig.setCurrentFavorite(iniConfig.getCurrentFavorite());
		dbConfig.setFavorites(newFavoritesList);

		ArrayList<DbFilterSection> newFilterList = new ArrayList<DbFilterSection>();
		for (IFilterSection f : iniConfig.getFilter()) {
			DbFilterSection newFilter = new DbFilterSection();
			newFilter.setDbConfig(dbConfig);
			newFilter
					.setFilter6581CurvePosition(f.getFilter6581CurvePosition());
			newFilter
					.setFilter8580CurvePosition(f.getFilter8580CurvePosition());
			newFilter.setName(f.getName());
			newFilterList.add(newFilter);
		}
		dbConfig.setFilter(newFilterList);
		em.persist(dbConfig);
		flush();
		return dbConfig;
	}

	private void flush() {
		em.getTransaction().begin();
		try {
			em.flush();
			em.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
	}

	public DbConfig restore(DbConfig config) {
		try {
			// import configuration from file
			JAXBContext jaxbContext = JAXBContext.newInstance(DbConfig.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			Object obj = unmarshaller.unmarshal(new File(config
					.getReconfigFilename()));
			if (obj instanceof DbConfig) {
				DbConfig detachedDbConfig = (DbConfig) obj;

				remove(config);

				// restore configuration in DB
				DbConfig mergedDbConfig = em.merge(detachedDbConfig);
				em.getTransaction().begin();
				em.persist(mergedDbConfig);
				em.flush();
				em.getTransaction().commit();
				return mergedDbConfig;
			}
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return config;
	}

}
