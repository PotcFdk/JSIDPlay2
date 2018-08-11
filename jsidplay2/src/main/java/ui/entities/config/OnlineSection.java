package ui.entities.config;

import javax.persistence.Embeddable;

@Embeddable
public class OnlineSection {

	private static final double DEF_ZOOM = 1.5;

	/**
	 * URL where the JSIDPlay2 is deployed to.
	 */
	private static final String DEPLOYMENT_URL = "https://haendel.ddns.net/~ken/";

	public static final String JSIDPLAY2_JS2_URL = DEPLOYMENT_URL + "jsidplay2.js2";
	public static final String JSIDPLAY2_APP_URL = DEPLOYMENT_URL + "jsidplay2app.apk";

	private String hvscUrl = DEPLOYMENT_URL + "online/hvsc/C64Music.zip";

	public String getHvscUrl() {
		return hvscUrl;
	}

	public void setHvscUrl(String hvscUrl) {
		this.hvscUrl = hvscUrl;
	}

	private String hvscSearchIndexUrl = DEPLOYMENT_URL + "online/hvsc/HVSC.script";

	public String getHvscSearchIndexUrl() {
		return hvscSearchIndexUrl;
	}

	public void setHvscSearchIndexUrl(String hvscSearchIndexUrl) {
		this.hvscSearchIndexUrl = hvscSearchIndexUrl;
	}
	
	private String hvscSearchIndexPropertiesUrl = DEPLOYMENT_URL + "online/hvsc/HVSC.properties";

	public String getHvscSearchIndexPropertiesUrl() {
		return hvscSearchIndexPropertiesUrl;
	}
	
	public void setHvscSearchIndexPropertiesUrl(String hvscSearchIndexPropertiesUrl) {
		this.hvscSearchIndexPropertiesUrl = hvscSearchIndexPropertiesUrl;
	}
	
	private String cgscUrl = DEPLOYMENT_URL + "online/cgsc/CGSC.zip";

	public String getCgscUrl() {
		return cgscUrl;
	}

	public void setCgscUrl(String cgscUrl) {
		this.cgscUrl = cgscUrl;
	}

	private String cgscSearchIndexUrl = DEPLOYMENT_URL + "online/cgsc/CGSC.script";

	public String getCgscSearchIndexUrl() {
		return cgscSearchIndexUrl;
	}
	public void setCgscSearchIndexUrl(String cgscSearchIndexUrl) {
		this.cgscSearchIndexUrl = cgscSearchIndexUrl;
	}
	
	public String getCgscSearchIndexPropertiesUrl() {
		return cgscSearchIndexPropertiesUrl;
	}
	public void setCgscSearchIndexPropertiesUrl(String cgscSearchIndexPropertiesUrl) {
		this.cgscSearchIndexPropertiesUrl = cgscSearchIndexPropertiesUrl;
	}
	
	private String cgscSearchIndexPropertiesUrl = DEPLOYMENT_URL + "online/cgsc/CGSC.properties";

	private String hvmecUrl = DEPLOYMENT_URL + "online/hvmec/HVMEC.zip";

	public String getHvmecUrl() {
		return hvmecUrl;
	}

	public void setHvmecUrl(String hvmecUrl) {
		this.hvmecUrl = hvmecUrl;
	}

	private String demosUrl = DEPLOYMENT_URL + "online/demos/Demos.zip";

	public String getDemosUrl() {
		return demosUrl;
	}

	public void setDemosUrl(String demosUrl) {
		this.demosUrl = demosUrl;
	}

	private String magazinesUrl = DEPLOYMENT_URL + "online/magazines/C64Magazines.zip";

	public String getMagazinesUrl() {
		return magazinesUrl;
	}

	public void setMagazinesUrl(String magazinesUrl) {
		this.magazinesUrl = magazinesUrl;
	}

	private String gamebaseUrl = DEPLOYMENT_URL + "online/gamebase/GameBase64.zip";

	public String getGamebaseUrl() {
		return gamebaseUrl;
	}

	public void setGamebaseUrl(String gamebaseUrl) {
		this.gamebaseUrl = gamebaseUrl;
	}

	private static final String SOASC_BASE = "http://www.se2a1.net/soasc/dl.php?d=";

	private String soasc6581R2 = SOASC_BASE + "soasc/soasc_mp3{0}_T{1,number,00}.sid_MOS6581R2.mp3";

	public String getSoasc6581R2() {
		return soasc6581R2;
	}

	public void setSoasc6581R2(String soasc6581r2) {
		soasc6581R2 = soasc6581r2;
	}

	private String soasc6581R4 = SOASC_BASE + "soasc/soasc_mp3{0}_T{1,number,00}.sid_MOS6581R4.mp3";

	public String getSoasc6581R4() {
		return soasc6581R4;
	}

	public void setSoasc6581R4(String soasc6581r4) {
		soasc6581R4 = soasc6581r4;
	}

	private String soasc8580R5 = SOASC_BASE + "soasc/soasc_mp3{0}_T{1,number,00}.sid_CSG8580R5.mp3";

	public String getSoasc8580R5() {
		return soasc8580R5;
	}

	public void setSoasc8580R5(String soasc8580r5) {
		soasc8580R5 = soasc8580r5;
	}

	private String gb64MusicUrl = DEPLOYMENT_URL+"music/";

	public String getGb64MusicUrl() {
		return gb64MusicUrl;
	}
	
	public void setGb64MusicUrl(String gb64MusicUrl) {
		this.gb64MusicUrl = gb64MusicUrl;
	}
	
	private String gb64ScreenshotUrl = DEPLOYMENT_URL+"screenshots/";

	public String getGb64ScreenshotUrl() {
		return gb64ScreenshotUrl;
	}
	
	public void setGb64ScreenshotUrl(String gb64ScreenshotUrl) {
		this.gb64ScreenshotUrl = gb64ScreenshotUrl;
	}
	
	private String gb64GamesUrl = DEPLOYMENT_URL+"games/";

	public String getGb64GamesUrl() {
		return gb64GamesUrl;
	}
	
	public void setGb64GamesUrl(String gb64GamesUrl) {
		this.gb64GamesUrl = gb64GamesUrl;
	}
	
	private double zoom = DEF_ZOOM;

	public double getZoom() {
		return zoom;
	}

	public void setZoom(double zoom) {
		this.zoom = zoom;
	}
}
