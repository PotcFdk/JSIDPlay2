package ui.entities.config;

import javax.persistence.Embeddable;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import ui.common.properties.ShadowField;

@Embeddable
public class OnlineSection {

	/**
	 * URL where the JSIDPlay2 is deployed to.
	 */
	private static final String DEPLOYMENT_URL = "https://haendel.ddns.net/~ken/";

	public static final String JSIDPLAY2_JS2_URL = DEPLOYMENT_URL + "jsidplay2.js2";

	public static final String JSIDPLAY2_APP_URL = DEPLOYMENT_URL + "jsidplay2app.apk";

	/**
	 * URL of the JSIDPlay2 AppServer.
	 */
	private static final String APP_SERVER_URL = "https://haendel.ddns.net:8443/";

	public static final String ONLINE_PLAYER_URL = APP_SERVER_URL + "static/hvsc.vue";

	/**
	 * URL where SOASC downloads are located.
	 */
	private static final String SOASC_BASE = "https://www.6581-8580.com/socse/dl.php?d=";

	/**
	 * URL where Assembly64 services are located.
	 */
	public static final String DEFAULT_ASSEMBLY64_URL = "https://hackerswithstyle.se";

	public static final double DEFAULT_ZOOM = 1.5;

	private ShadowField<StringProperty, String> hvscUrl = new ShadowField<>(SimpleStringProperty::new,
			DEPLOYMENT_URL + "online/hvsc/C64Music.zip");

	public String getHvscUrl() {
		return hvscUrl.get();
	}

	public void setHvscUrl(String hvscUrl) {
		this.hvscUrl.set(hvscUrl);
	}

	public StringProperty hvscUrlProperty() {
		return hvscUrl.property();
	}

	private ShadowField<StringProperty, String> hvscSearchIndexUrl = new ShadowField<>(SimpleStringProperty::new,
			DEPLOYMENT_URL + "online/hvsc/HVSC.script");

	public String getHvscSearchIndexUrl() {
		return hvscSearchIndexUrl.get();
	}

	public void setHvscSearchIndexUrl(String hvscSearchIndexUrl) {
		this.hvscSearchIndexUrl.set(hvscSearchIndexUrl);
	}

	public StringProperty hvscSearchIndexUrlProperty() {
		return hvscSearchIndexUrl.property();
	}

	private ShadowField<StringProperty, String> hvscSearchIndexPropertiesUrl = new ShadowField<>(
			SimpleStringProperty::new, DEPLOYMENT_URL + "online/hvsc/HVSC.properties");

	public String getHvscSearchIndexPropertiesUrl() {
		return hvscSearchIndexPropertiesUrl.get();
	}

	public void setHvscSearchIndexPropertiesUrl(String hvscSearchIndexPropertiesUrl) {
		this.hvscSearchIndexPropertiesUrl.set(hvscSearchIndexPropertiesUrl);
	}

	public StringProperty hvscSearchIndexPropertiesUrlProperty() {
		return hvscSearchIndexPropertiesUrl.property();
	}

	private ShadowField<StringProperty, String> cgscUrl = new ShadowField<>(SimpleStringProperty::new,
			DEPLOYMENT_URL + "online/cgsc/CGSC.zip");

	public String getCgscUrl() {
		return cgscUrl.get();
	}

	public void setCgscUrl(String cgscUrl) {
		this.cgscUrl.set(cgscUrl);
	}

	public StringProperty cgscUrlProperty() {
		return cgscUrl.property();
	}

	private ShadowField<StringProperty, String> cgscSearchIndexUrl = new ShadowField<>(SimpleStringProperty::new,
			DEPLOYMENT_URL + "online/cgsc/CGSC.script");

	public String getCgscSearchIndexUrl() {
		return cgscSearchIndexUrl.get();
	}

	public void setCgscSearchIndexUrl(String cgscSearchIndexUrl) {
		this.cgscSearchIndexUrl.set(cgscSearchIndexUrl);
	}

	public StringProperty cgscSearchIndexUrlProperty() {
		return cgscSearchIndexUrl.property();
	}

	private ShadowField<StringProperty, String> cgscSearchIndexPropertiesUrl = new ShadowField<>(
			SimpleStringProperty::new, DEPLOYMENT_URL + "online/cgsc/CGSC.properties");

	public String getCgscSearchIndexPropertiesUrl() {
		return cgscSearchIndexPropertiesUrl.get();
	}

	public void setCgscSearchIndexPropertiesUrl(String cgscSearchIndexPropertiesUrl) {
		this.cgscSearchIndexPropertiesUrl.set(cgscSearchIndexPropertiesUrl);
	}

	public StringProperty cgscSearchIndexPropertiesUrlProperty() {
		return cgscSearchIndexPropertiesUrl.property();
	}

	private ShadowField<StringProperty, String> hvmecUrl = new ShadowField<>(SimpleStringProperty::new,
			DEPLOYMENT_URL + "online/hvmec/HVMEC.zip");

	public String getHvmecUrl() {
		return hvmecUrl.get();
	}

	public void setHvmecUrl(String hvmecUrl) {
		this.hvmecUrl.set(hvmecUrl);
	}

	public StringProperty hvmecUrlProperty() {
		return hvmecUrl.property();
	}

	private ShadowField<StringProperty, String> demosUrl = new ShadowField<>(SimpleStringProperty::new,
			DEPLOYMENT_URL + "online/demos/Demos.zip");

	public String getDemosUrl() {
		return demosUrl.get();
	}

	public void setDemosUrl(String demosUrl) {
		this.demosUrl.set(demosUrl);
	}

	public StringProperty demosUrlProperty() {
		return demosUrl.property();
	}

	private ShadowField<StringProperty, String> magazinesUrl = new ShadowField<>(SimpleStringProperty::new,
			DEPLOYMENT_URL + "online/magazines/C64Magazines.zip");

	public String getMagazinesUrl() {
		return magazinesUrl.get();
	}

	public void setMagazinesUrl(String magazinesUrl) {
		this.magazinesUrl.set(magazinesUrl);
	}

	public StringProperty magazinesUrlProperty() {
		return magazinesUrl.property();
	}

	private ShadowField<StringProperty, String> gamebaseUrl = new ShadowField<>(SimpleStringProperty::new,
			DEPLOYMENT_URL + "online/gamebase/GameBase64.zip");

	public String getGamebaseUrl() {
		return gamebaseUrl.get();
	}

	public void setGamebaseUrl(String gamebaseUrl) {
		this.gamebaseUrl.set(gamebaseUrl);
	}

	public StringProperty gamebaseUrlProperty() {
		return gamebaseUrl.property();
	}

	private ShadowField<StringProperty, String> soasc6581R2 = new ShadowField<>(SimpleStringProperty::new,
			SOASC_BASE + "soasc/hvsc/{0,number,000}/MP3{1}_T{2,number,000}.sid_MOS6581R2.mp3");

	public String getSoasc6581R2() {
		return soasc6581R2.get();
	}

	public void setSoasc6581R2(String soasc6581r2) {
		soasc6581R2.set(soasc6581r2);
	}

	public StringProperty soasc6581R2Property() {
		return soasc6581R2.property();
	}

	private ShadowField<StringProperty, String> soasc6581R3 = new ShadowField<>(SimpleStringProperty::new,
			SOASC_BASE + "soasc/hvsc/{0,number,000}/MP3{1}_T{2,number,000}.sid_MOS6581R3.mp3");

	public String getSoasc6581R3() {
		return soasc6581R3.get();
	}

	public void setSoasc6581R3(String soasc6581r3) {
		soasc6581R3.set(soasc6581r3);
	}

	public StringProperty soasc6581R3Property() {
		return soasc6581R3.property();
	}

	private ShadowField<StringProperty, String> soasc6581R4 = new ShadowField<>(SimpleStringProperty::new,
			SOASC_BASE + "soasc/hvsc/{0,number,000}/MP3{1}_T{2,number,000}.sid_MOS6581R4.mp3");

	public String getSoasc6581R4() {
		return soasc6581R4.get();
	}

	public void setSoasc6581R4(String soasc6581r4) {
		soasc6581R4.set(soasc6581r4);
	}

	public StringProperty soasc6581R4Property() {
		return soasc6581R4.property();
	}

	private ShadowField<StringProperty, String> soasc8580R5 = new ShadowField<>(SimpleStringProperty::new,
			SOASC_BASE + "soasc/hvsc/{0,number,000}/MP3{1}_T{2,number,000}.sid_CSG8580R5.mp3");

	public String getSoasc8580R5() {
		return soasc8580R5.get();
	}

	public void setSoasc8580R5(String soasc8580r5) {
		soasc8580R5.set(soasc8580r5);
	}

	public StringProperty soasc8580R5Property() {
		return soasc8580R5.property();
	}

	private ShadowField<StringProperty, String> gb64MusicUrl = new ShadowField<>(SimpleStringProperty::new,
			DEPLOYMENT_URL + "music/");

	public String getGb64MusicUrl() {
		return gb64MusicUrl.get();
	}

	public void setGb64MusicUrl(String gb64MusicUrl) {
		this.gb64MusicUrl.set(gb64MusicUrl);
	}

	public StringProperty gb64MusicUrlProperty() {
		return gb64MusicUrl.property();
	}

	private ShadowField<StringProperty, String> gb64ScreenshotUrl = new ShadowField<>(SimpleStringProperty::new,
			DEPLOYMENT_URL + "screenshots/");

	public String getGb64ScreenshotUrl() {
		return gb64ScreenshotUrl.get();
	}

	public void setGb64ScreenshotUrl(String gb64ScreenshotUrl) {
		this.gb64ScreenshotUrl.set(gb64ScreenshotUrl);
	}

	public StringProperty gb64ScreenshotUrlProperty() {
		return gb64ScreenshotUrl.property();
	}

	private ShadowField<StringProperty, String> gb64GamesUrl = new ShadowField<>(SimpleStringProperty::new,
			DEPLOYMENT_URL + "games/");

	public String getGb64GamesUrl() {
		return gb64GamesUrl.get();
	}

	public void setGb64GamesUrl(String gb64GamesUrl) {
		this.gb64GamesUrl.set(gb64GamesUrl);
	}

	public StringProperty gb64GamesUrlProperty() {
		return gb64GamesUrl.property();
	}

	private ShadowField<StringProperty, String> assembly64Url = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ASSEMBLY64_URL);

	public String getAssembly64Url() {
		return assembly64Url.get();
	}

	public void setAssembly64Url(String assembly64Url) {
		this.assembly64Url.set(assembly64Url);
	}

	public StringProperty assembly64UrlProperty() {
		return assembly64Url.property();
	}

	private ShadowField<DoubleProperty, Number> zoom = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_ZOOM);

	public double getZoom() {
		return zoom.get().doubleValue();
	}

	public void setZoom(double zoom) {
		this.zoom.set(zoom);
	}

	public DoubleProperty zoomProperty() {
		return zoom.property();
	}

}
