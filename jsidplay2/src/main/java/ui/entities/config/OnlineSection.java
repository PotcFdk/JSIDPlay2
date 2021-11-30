package ui.entities.config;

import javax.persistence.Embeddable;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.properties.ShadowField;

@Embeddable
public class OnlineSection {

	/**
	 * URL where the JSIDPlay2 is deployed to.
	 */
	public static final String DEPLOYMENT_URL = "https://haendel.ddns.net/~ken/";

	public static final String JSIDPLAY2_APP_URL = DEPLOYMENT_URL + "jsidplay2app.apk";
	public static final String JSIDPLAY2_SIDBLASTER_DOC_URL = DEPLOYMENT_URL + "sidblaster.html";
	public static final String JSIDPLAY2_C64MUSIC_URL = DEPLOYMENT_URL + "online/hvsc/C64Music.zip";
	public static final String JSIDPLAY2_HVSC_SCRIPT_URL = DEPLOYMENT_URL + "online/hvsc/HVSC.script";
	public static final String JSIDPLAY2_HVSC_PROPERTIES_URL = DEPLOYMENT_URL + "online/hvsc/HVSC.properties";
	public static final String JSIDPLAY2_CGSC_URL = DEPLOYMENT_URL + "online/cgsc/CGSC.zip";
	public static final String JSIDPLAY2_CGSC_SCRIPT_URL = DEPLOYMENT_URL + "online/cgsc/CGSC.script";
	public static final String JSIDPLAY2_CGSC_PROPERTIES_URL = DEPLOYMENT_URL + "online/cgsc/CGSC.properties";
	public static final String JSIDPLAY2_HVMEC_URL = DEPLOYMENT_URL + "online/hvmec/HVMEC.zip";
	public static final String JSIDPLAY2_DEMOS_URL = DEPLOYMENT_URL + "online/demos/Demos.zip";
	public static final String JSIDPLAY2_C64MAGAZINES_URL = DEPLOYMENT_URL + "online/magazines/C64Magazines.zip";
	public static final String JSIDPLAY2_GAMEBASE64_URL = DEPLOYMENT_URL + "online/gamebase/GameBase64.zip";
	public static final String JSIDPLAY2_GAMEBASE64_MUSIC_URL = DEPLOYMENT_URL + "music/";
	public static final String JSIDPLAY2_GAMEBASE64_SCREENSHOTS_URL = DEPLOYMENT_URL + "screenshots/";
	public static final String JSIDPLAY2_GAMEBASE64_PHOTOS_URL = DEPLOYMENT_URL + "photos/";
	public static final String JSIDPLAY2_GAMEBASE64_GAMES_URL = DEPLOYMENT_URL + "games/";
	public static final String JSIDPLAY2_FAVORITES_URL = DEPLOYMENT_URL + "jsidplay2.js2";

	/**
	 * URL of the JSIDPlay2 AppServer.
	 */
	public static final String APP_SERVER_URL = "https://haendel.ddns.net:8443/";

	public static final String ONLINE_PLAYER_URL = APP_SERVER_URL + "static/hvsc.vue";

	/**
	 * URL where SOASC downloads are located.
	 */
	public static final String SOASC_BASE = "https://www.6581-8580.com/socse/dl.php?d=";

	public static final String SOASC_8580_R5_URL = SOASC_BASE
			+ "soasc/hvsc/{0,number,000}/MP3{1}_T{2,number,000}.sid_CSG8580R5.mp3";
	public static final String SOASC_6581_R4_URL = SOASC_BASE
			+ "soasc/hvsc/{0,number,000}/MP3{1}_T{2,number,000}.sid_MOS6581R4.mp3";
	public static final String SOASC_6581_R3_URL = SOASC_BASE
			+ "soasc/hvsc/{0,number,000}/MP3{1}_T{2,number,000}.sid_MOS6581R3.mp3";
	public static final String SOASC_6581_R2_URL = SOASC_BASE
			+ "soasc/hvsc/{0,number,000}/MP3{1}_T{2,number,000}.sid_MOS6581R2.mp3";

	/**
	 * URL where Assembly64 services are located.
	 */
	public static final String DEFAULT_ASSEMBLY64_URL = "https://hackerswithstyle.se";

	public static final double DEFAULT_ZOOM = 1.5;

	private ShadowField<StringProperty, String> hvscUrl = new ShadowField<>(SimpleStringProperty::new,
			JSIDPLAY2_C64MUSIC_URL);

	public final String getHvscUrl() {
		return hvscUrl.get();
	}

	public final void setHvscUrl(String hvscUrl) {
		this.hvscUrl.set(hvscUrl);
	}

	public final StringProperty hvscUrlProperty() {
		return hvscUrl.property();
	}

	private ShadowField<StringProperty, String> hvscSearchIndexUrl = new ShadowField<>(SimpleStringProperty::new,
			JSIDPLAY2_HVSC_SCRIPT_URL);

	public final String getHvscSearchIndexUrl() {
		return hvscSearchIndexUrl.get();
	}

	public final void setHvscSearchIndexUrl(String hvscSearchIndexUrl) {
		this.hvscSearchIndexUrl.set(hvscSearchIndexUrl);
	}

	public final StringProperty hvscSearchIndexUrlProperty() {
		return hvscSearchIndexUrl.property();
	}

	private ShadowField<StringProperty, String> hvscSearchIndexPropertiesUrl = new ShadowField<>(
			SimpleStringProperty::new, JSIDPLAY2_HVSC_PROPERTIES_URL);

	public final String getHvscSearchIndexPropertiesUrl() {
		return hvscSearchIndexPropertiesUrl.get();
	}

	public final void setHvscSearchIndexPropertiesUrl(String hvscSearchIndexPropertiesUrl) {
		this.hvscSearchIndexPropertiesUrl.set(hvscSearchIndexPropertiesUrl);
	}

	public final StringProperty hvscSearchIndexPropertiesUrlProperty() {
		return hvscSearchIndexPropertiesUrl.property();
	}

	private ShadowField<StringProperty, String> cgscUrl = new ShadowField<>(SimpleStringProperty::new,
			JSIDPLAY2_CGSC_URL);

	public final String getCgscUrl() {
		return cgscUrl.get();
	}

	public final void setCgscUrl(String cgscUrl) {
		this.cgscUrl.set(cgscUrl);
	}

	public final StringProperty cgscUrlProperty() {
		return cgscUrl.property();
	}

	private ShadowField<StringProperty, String> cgscSearchIndexUrl = new ShadowField<>(SimpleStringProperty::new,
			JSIDPLAY2_CGSC_SCRIPT_URL);

	public final String getCgscSearchIndexUrl() {
		return cgscSearchIndexUrl.get();
	}

	public final void setCgscSearchIndexUrl(String cgscSearchIndexUrl) {
		this.cgscSearchIndexUrl.set(cgscSearchIndexUrl);
	}

	public final StringProperty cgscSearchIndexUrlProperty() {
		return cgscSearchIndexUrl.property();
	}

	private ShadowField<StringProperty, String> cgscSearchIndexPropertiesUrl = new ShadowField<>(
			SimpleStringProperty::new, JSIDPLAY2_CGSC_PROPERTIES_URL);

	public final String getCgscSearchIndexPropertiesUrl() {
		return cgscSearchIndexPropertiesUrl.get();
	}

	public final void setCgscSearchIndexPropertiesUrl(String cgscSearchIndexPropertiesUrl) {
		this.cgscSearchIndexPropertiesUrl.set(cgscSearchIndexPropertiesUrl);
	}

	public final StringProperty cgscSearchIndexPropertiesUrlProperty() {
		return cgscSearchIndexPropertiesUrl.property();
	}

	private ShadowField<StringProperty, String> hvmecUrl = new ShadowField<>(SimpleStringProperty::new,
			JSIDPLAY2_HVMEC_URL);

	public final String getHvmecUrl() {
		return hvmecUrl.get();
	}

	public final void setHvmecUrl(String hvmecUrl) {
		this.hvmecUrl.set(hvmecUrl);
	}

	public final StringProperty hvmecUrlProperty() {
		return hvmecUrl.property();
	}

	private ShadowField<StringProperty, String> demosUrl = new ShadowField<>(SimpleStringProperty::new,
			JSIDPLAY2_DEMOS_URL);

	public final String getDemosUrl() {
		return demosUrl.get();
	}

	public final void setDemosUrl(String demosUrl) {
		this.demosUrl.set(demosUrl);
	}

	public final StringProperty demosUrlProperty() {
		return demosUrl.property();
	}

	private ShadowField<StringProperty, String> magazinesUrl = new ShadowField<>(SimpleStringProperty::new,
			JSIDPLAY2_C64MAGAZINES_URL);

	public final String getMagazinesUrl() {
		return magazinesUrl.get();
	}

	public final void setMagazinesUrl(String magazinesUrl) {
		this.magazinesUrl.set(magazinesUrl);
	}

	public final StringProperty magazinesUrlProperty() {
		return magazinesUrl.property();
	}

	private ShadowField<StringProperty, String> gamebaseUrl = new ShadowField<>(SimpleStringProperty::new,
			JSIDPLAY2_GAMEBASE64_URL);

	public final String getGamebaseUrl() {
		return gamebaseUrl.get();
	}

	public final void setGamebaseUrl(String gamebaseUrl) {
		this.gamebaseUrl.set(gamebaseUrl);
	}

	public final StringProperty gamebaseUrlProperty() {
		return gamebaseUrl.property();
	}

	private ShadowField<StringProperty, String> soasc6581R2 = new ShadowField<>(SimpleStringProperty::new,
			SOASC_6581_R2_URL);

	public final String getSoasc6581R2() {
		return soasc6581R2.get();
	}

	public final void setSoasc6581R2(String soasc6581r2) {
		soasc6581R2.set(soasc6581r2);
	}

	public final StringProperty soasc6581R2Property() {
		return soasc6581R2.property();
	}

	private ShadowField<StringProperty, String> soasc6581R3 = new ShadowField<>(SimpleStringProperty::new,
			SOASC_6581_R3_URL);

	public final String getSoasc6581R3() {
		return soasc6581R3.get();
	}

	public final void setSoasc6581R3(String soasc6581r3) {
		soasc6581R3.set(soasc6581r3);
	}

	public final StringProperty soasc6581R3Property() {
		return soasc6581R3.property();
	}

	private ShadowField<StringProperty, String> soasc6581R4 = new ShadowField<>(SimpleStringProperty::new,
			SOASC_6581_R4_URL);

	public final String getSoasc6581R4() {
		return soasc6581R4.get();
	}

	public final void setSoasc6581R4(String soasc6581r4) {
		soasc6581R4.set(soasc6581r4);
	}

	public final StringProperty soasc6581R4Property() {
		return soasc6581R4.property();
	}

	private ShadowField<StringProperty, String> soasc8580R5 = new ShadowField<>(SimpleStringProperty::new,
			SOASC_8580_R5_URL);

	public final String getSoasc8580R5() {
		return soasc8580R5.get();
	}

	public final void setSoasc8580R5(String soasc8580r5) {
		soasc8580R5.set(soasc8580r5);
	}

	public final StringProperty soasc8580R5Property() {
		return soasc8580R5.property();
	}

	private ShadowField<StringProperty, String> gb64MusicUrl = new ShadowField<>(SimpleStringProperty::new,
			JSIDPLAY2_GAMEBASE64_MUSIC_URL);

	public final String getGb64MusicUrl() {
		return gb64MusicUrl.get();
	}

	public final void setGb64MusicUrl(String gb64MusicUrl) {
		this.gb64MusicUrl.set(gb64MusicUrl);
	}

	public final StringProperty gb64MusicUrlProperty() {
		return gb64MusicUrl.property();
	}

	private ShadowField<StringProperty, String> gb64ScreenshotUrl = new ShadowField<>(SimpleStringProperty::new,
			JSIDPLAY2_GAMEBASE64_SCREENSHOTS_URL);

	public final String getGb64ScreenshotUrl() {
		return gb64ScreenshotUrl.get();
	}

	public final void setGb64ScreenshotUrl(String gb64ScreenshotUrl) {
		this.gb64ScreenshotUrl.set(gb64ScreenshotUrl);
	}

	public final StringProperty gb64ScreenshotUrlProperty() {
		return gb64ScreenshotUrl.property();
	}

	private ShadowField<StringProperty, String> gb64PhotosUrl = new ShadowField<>(SimpleStringProperty::new,
			JSIDPLAY2_GAMEBASE64_PHOTOS_URL);

	public final String getGb64PhotosUrl() {
		return gb64PhotosUrl.get();
	}

	public final void setGb64PhotosUrl(String gb64PhotosUrl) {
		this.gb64PhotosUrl.set(gb64PhotosUrl);
	}

	public final StringProperty gb64PhotosUrlProperty() {
		return gb64PhotosUrl.property();
	}

	private ShadowField<StringProperty, String> gb64GamesUrl = new ShadowField<>(SimpleStringProperty::new,
			JSIDPLAY2_GAMEBASE64_GAMES_URL);

	public final String getGb64GamesUrl() {
		return gb64GamesUrl.get();
	}

	public final void setGb64GamesUrl(String gb64GamesUrl) {
		this.gb64GamesUrl.set(gb64GamesUrl);
	}

	public final StringProperty gb64GamesUrlProperty() {
		return gb64GamesUrl.property();
	}

	private ShadowField<StringProperty, String> assembly64Url = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ASSEMBLY64_URL);

	public final String getAssembly64Url() {
		return assembly64Url.get();
	}

	public final void setAssembly64Url(String assembly64Url) {
		this.assembly64Url.set(assembly64Url);
	}

	public final StringProperty assembly64UrlProperty() {
		return assembly64Url.property();
	}

	private ShadowField<StringProperty, String> favoritesUrl = new ShadowField<>(SimpleStringProperty::new,
			JSIDPLAY2_FAVORITES_URL);

	public final String getFavoritesUrl() {
		return favoritesUrl.get();
	}

	public final void setFavoritesUrl(String favoritesUrl) {
		this.favoritesUrl.set(favoritesUrl);
	}

	public final StringProperty favoritesUrlProperty() {
		return favoritesUrl.property();
	}

	private ShadowField<StringProperty, String> appUrl = new ShadowField<>(SimpleStringProperty::new,
			JSIDPLAY2_APP_URL);

	public final String getAppUrl() {
		return appUrl.get();
	}

	public final void setAppUrl(String appUrl) {
		this.appUrl.set(appUrl);
	}

	public final StringProperty appUrlProperty() {
		return appUrl.property();
	}

	private ShadowField<StringProperty, String> onlinePlayerUrl = new ShadowField<>(SimpleStringProperty::new,
			ONLINE_PLAYER_URL);

	public final String getOnlinePlayerUrl() {
		return onlinePlayerUrl.get();
	}

	public final void setOnlinePlayerUrl(String onlinePlayerUrl) {
		this.onlinePlayerUrl.set(onlinePlayerUrl);
	}

	public final StringProperty onlinePlayerUrlProperty() {
		return onlinePlayerUrl.property();
	}

	private ShadowField<StringProperty, String> sidBlasterDocUrl = new ShadowField<>(SimpleStringProperty::new,
			JSIDPLAY2_SIDBLASTER_DOC_URL);

	public final String getSidBlasterDocUrl() {
		return sidBlasterDocUrl.get();
	}

	public final void setSidBlasterDocUrl(String sidBlasterDocUrl) {
		this.sidBlasterDocUrl.set(sidBlasterDocUrl);
	}

	public final StringProperty sidBlasterDocUrlProperty() {
		return sidBlasterDocUrl.property();
	}

	private ShadowField<DoubleProperty, Number> zoom = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_ZOOM);

	public final double getZoom() {
		return zoom.get().doubleValue();
	}

	public final void setZoom(double zoom) {
		this.zoom.set(zoom);
	}

	public final DoubleProperty zoomProperty() {
		return zoom.property();
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
