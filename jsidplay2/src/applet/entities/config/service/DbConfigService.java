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
import applet.entities.config.DbPrinterSection;
import applet.entities.config.DbSidPlay2Section;

public class DbConfigService {
	private EntityManager em;

	public DbConfigService(EntityManager em) {
		this.em = em;
	};

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
		dbConfig.setSidplay2(new DbSidPlay2Section());
		dbConfig.getSidplay2().setEnableDatabase(
				iniConfig.getSidplay2().isEnableDatabase());
		dbConfig.getSidplay2().setPlayLength(
				iniConfig.getSidplay2().getPlayLength());
		dbConfig.getSidplay2().setRecordLength(
				iniConfig.getSidplay2().getRecordLength());
		dbConfig.getSidplay2().setHVMEC(iniConfig.getSidplay2().getHVMEC());
		dbConfig.getSidplay2().setDemos(iniConfig.getSidplay2().getDemos());
		dbConfig.getSidplay2().setMags(iniConfig.getSidplay2().getMags());
		dbConfig.getSidplay2().setCgsc(iniConfig.getSidplay2().getCgsc());
		dbConfig.getSidplay2().setHvsc(iniConfig.getSidplay2().getHvsc());
		dbConfig.getSidplay2().setSingle(iniConfig.getSidplay2().isSingle());
		dbConfig.getSidplay2().setSoasc6581R2(
				iniConfig.getSidplay2().getSoasc6581R2());
		dbConfig.getSidplay2().setSoasc6581R4(
				iniConfig.getSidplay2().getSoasc6581R4());
		dbConfig.getSidplay2().setSoasc8580R5(
				iniConfig.getSidplay2().getSoasc8580R5());
		dbConfig.getSidplay2().setEnableProxy(
				iniConfig.getSidplay2().isEnableProxy());
		dbConfig.getSidplay2().setProxyHostname(
				iniConfig.getSidplay2().getProxyHostname());
		dbConfig.getSidplay2().setProxyPort(
				iniConfig.getSidplay2().getProxyPort());
		dbConfig.getSidplay2().setLastDirectory(
				iniConfig.getSidplay2().getLastDirectory());
		dbConfig.getSidplay2().setTmpDir(iniConfig.getSidplay2().getTmpDir());
		dbConfig.getSidplay2().setFrameX(iniConfig.getSidplay2().getFrameX());
		dbConfig.getSidplay2().setFrameY(iniConfig.getSidplay2().getFrameY());
		dbConfig.getSidplay2().setFrameWidth(
				iniConfig.getSidplay2().getFrameWidth());
		dbConfig.getSidplay2().setFrameHeight(
				iniConfig.getSidplay2().getFrameHeight());

		dbConfig.setC1541(new DbC1541Section());
		dbConfig.getC1541().setDriveOn(iniConfig.getC1541().isDriveOn());
		dbConfig.getC1541().setDriveSoundOn(
				iniConfig.getC1541().isDriveSoundOn());
		dbConfig.getC1541().setParallelCable(
				iniConfig.getC1541().isParallelCable());
		dbConfig.getC1541().setRamExpansion0(
				iniConfig.getC1541().isRamExpansionEnabled0());
		dbConfig.getC1541().setRamExpansion1(
				iniConfig.getC1541().isRamExpansionEnabled1());
		dbConfig.getC1541().setRamExpansion2(
				iniConfig.getC1541().isRamExpansionEnabled2());
		dbConfig.getC1541().setRamExpansion3(
				iniConfig.getC1541().isRamExpansionEnabled3());
		dbConfig.getC1541().setRamExpansion4(
				iniConfig.getC1541().isRamExpansionEnabled4());
		dbConfig.getC1541().setExtendImagePolicy(
				iniConfig.getC1541().getExtendImagePolicy());
		dbConfig.getC1541().setFloppyType(iniConfig.getC1541().getFloppyType());

		dbConfig.setPrinter(new DbPrinterSection());
		dbConfig.getPrinter()
				.setPrinterOn(iniConfig.getPrinter().isPrinterOn());

		dbConfig.setJoystick(new DbJoystickSection());
		dbConfig.getJoystick().setDeviceName1(
				iniConfig.getJoystick().getDeviceName1());
		dbConfig.getJoystick().setDeviceName2(
				iniConfig.getJoystick().getDeviceName2());
		dbConfig.getJoystick().setComponentNameUp1(
				iniConfig.getJoystick().getComponentNameUp1());
		dbConfig.getJoystick().setComponentNameUp2(
				iniConfig.getJoystick().getComponentNameUp2());
		dbConfig.getJoystick().setComponentNameDown1(
				iniConfig.getJoystick().getComponentNameDown1());
		dbConfig.getJoystick().setComponentNameDown2(
				iniConfig.getJoystick().getComponentNameDown2());
		dbConfig.getJoystick().setComponentNameLeft1(
				iniConfig.getJoystick().getComponentNameLeft1());
		dbConfig.getJoystick().setComponentNameLeft2(
				iniConfig.getJoystick().getComponentNameLeft2());
		dbConfig.getJoystick().setComponentNameRight1(
				iniConfig.getJoystick().getComponentNameRight1());
		dbConfig.getJoystick().setComponentNameRight2(
				iniConfig.getJoystick().getComponentNameRight2());
		dbConfig.getJoystick().setComponentNameBtn1(
				iniConfig.getJoystick().getComponentNameBtn1());
		dbConfig.getJoystick().setComponentNameBtn2(
				iniConfig.getJoystick().getComponentNameBtn2());
		dbConfig.getJoystick().setComponentValueUp1(
				iniConfig.getJoystick().getComponentValueUp1());
		dbConfig.getJoystick().setComponentValueUp2(
				iniConfig.getJoystick().getComponentValueUp2());
		dbConfig.getJoystick().setComponentValueDown1(
				iniConfig.getJoystick().getComponentValueDown1());
		dbConfig.getJoystick().setComponentValueDown2(
				iniConfig.getJoystick().getComponentValueDown2());
		dbConfig.getJoystick().setComponentValueLeft1(
				iniConfig.getJoystick().getComponentValueLeft1());
		dbConfig.getJoystick().setComponentValueLeft2(
				iniConfig.getJoystick().getComponentValueLeft2());
		dbConfig.getJoystick().setComponentValueRight1(
				iniConfig.getJoystick().getComponentValueRight1());
		dbConfig.getJoystick().setComponentValueRight2(
				iniConfig.getJoystick().getComponentValueRight2());
		dbConfig.getJoystick().setComponentValueBtn1(
				iniConfig.getJoystick().getComponentValueBtn1());
		dbConfig.getJoystick().setComponentValueBtn2(
				iniConfig.getJoystick().getComponentValueBtn2());

		dbConfig.setConsole(new DbConsoleSection());
		dbConfig.getConsole().setBottomLeft(
				iniConfig.getConsole().getBottomLeft());
		dbConfig.getConsole().setBottomRight(
				iniConfig.getConsole().getBottomRight());
		dbConfig.getConsole().setHorizontal(
				iniConfig.getConsole().getHorizontal());
		dbConfig.getConsole().setVertical(iniConfig.getConsole().getVertical());
		dbConfig.getConsole().setJunctionLeft(
				iniConfig.getConsole().getJunctionLeft());
		dbConfig.getConsole().setJunctionRight(
				iniConfig.getConsole().getJunctionRight());
		dbConfig.getConsole().setTopLeft(iniConfig.getConsole().getTopLeft());
		dbConfig.getConsole().setTopRight(iniConfig.getConsole().getTopRight());

		dbConfig.setAudio(new DbAudioSection());
		dbConfig.getAudio().setFrequency(iniConfig.getAudio().getFrequency());
		dbConfig.getAudio().setSampling(iniConfig.getAudio().getSampling());
		dbConfig.getAudio().setPlayOriginal(
				iniConfig.getAudio().isPlayOriginal());
		dbConfig.getAudio().setMp3File(iniConfig.getAudio().getMp3File());
		dbConfig.getAudio().setLeftVolume(iniConfig.getAudio().getLeftVolume());
		dbConfig.getAudio().setRightVolume(
				iniConfig.getAudio().getRightVolume());

		dbConfig.setEmulation(new DbEmulationSection());
		dbConfig.getEmulation().setDefaultClockSpeed(
				iniConfig.getEmulation().getDefaultClockSpeed());
		dbConfig.getEmulation().setUserClockSpeed(
				iniConfig.getEmulation().getUserClockSpeed());
		dbConfig.getEmulation().setDefaultSidModel(
				iniConfig.getEmulation().getDefaultSidModel());
		dbConfig.getEmulation().setUserSidModel(
				iniConfig.getEmulation().getUserSidModel());
		dbConfig.getEmulation().setHardsid6581(
				iniConfig.getEmulation().getHardsid6581());
		dbConfig.getEmulation().setHardsid8580(
				iniConfig.getEmulation().getHardsid8580());
		dbConfig.getEmulation().setFilter(iniConfig.getEmulation().isFilter());
		dbConfig.getEmulation().setFilter6581(
				iniConfig.getEmulation().getFilter6581());
		dbConfig.getEmulation().setFilter8580(
				iniConfig.getEmulation().getFilter8580());
		dbConfig.getEmulation().setDigiBoosted8580(
				iniConfig.getEmulation().isDigiBoosted8580());
		dbConfig.getEmulation().setDualSidBase(
				iniConfig.getEmulation().getDualSidBase());
		dbConfig.getEmulation().setForceStereoTune(
				iniConfig.getEmulation().isForceStereoTune());
		dbConfig.getEmulation().setStereoSidModel(
				iniConfig.getEmulation().getStereoSidModel());

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

				// remove old configuration from DB
				em.getTransaction().begin();
				em.remove(config);
				em.flush();
				em.clear();
				// restore configuration in DB
				DbConfig mergedDbConfig = em.merge(detachedDbConfig);
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
