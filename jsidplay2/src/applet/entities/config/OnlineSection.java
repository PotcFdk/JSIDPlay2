package applet.entities.config;

import javax.persistence.Embeddable;

import sidplay.ini.intf.IOnlineSection;
import applet.config.annotations.ConfigDescription;
import applet.config.annotations.ConfigTransient;

@Embeddable
public class OnlineSection implements IOnlineSection {

	/**
	 * URL where the JSIDPlay2 is deployed to.
	 */
	@ConfigTransient
	private static final String DEPLOYMENT_URL = "http://kenchis.t15.org/jsidplay2/";

	@ConfigDescription(descriptionKey = "ONLINE_HVSC_URL_DESC", toolTipKey = "ONLINE_HVSC_URL_TOOLTIP")
	private String hvscUrl = DEPLOYMENT_URL + "online/hvsc/C64Music.zip";

	@Override
	public String getHvscUrl() {
		return hvscUrl;
	}

	public void setHvscUrl(String hvscUrl) {
		this.hvscUrl = hvscUrl;
	}

	@ConfigDescription(descriptionKey = "ONLINE_CGSC_URL_DESC", toolTipKey = "ONLINE_CGSC_URL_TOOLTIP")
	private String cgscUrl = DEPLOYMENT_URL + "online/cgsc/CGSC.zip";

	@Override
	public String getCgscUrl() {
		return cgscUrl;
	}

	public void setCgscurl(String cgscUrl) {
		this.cgscUrl = cgscUrl;
	}

	@ConfigDescription(descriptionKey = "ONLINE_HVMEC_URL_DESC", toolTipKey = "ONLINE_HVMEC_URL_TOOLTIP")
	private String hvmecUrl = DEPLOYMENT_URL + "online/hvmec/HVMEC.zip";

	@Override
	public String getHvmecUrl() {
		return hvmecUrl;
	}

	public void setHvmecUrl(String hvmecUrl) {
		this.hvmecUrl = hvmecUrl;
	}

	@ConfigDescription(descriptionKey = "ONLINE_DEMOS_URL_DESC", toolTipKey = "ONLINE_DEMOS_URL_TOOLTIP")
	private String demosUrl = DEPLOYMENT_URL + "online/demos/Demos.zip";

	@Override
	public String getDemosUrl() {
		return demosUrl;
	}

	public void setDemosUrl(String demosUrl) {
		this.demosUrl = demosUrl;
	}

	@ConfigDescription(descriptionKey = "ONLINE_MAGS_URL_DESC", toolTipKey = "ONLINE_MAGS_URL_TOOLTIP")
	private String magazinesUrl = DEPLOYMENT_URL
			+ "online/mags/C64Magazines.zip";

	@Override
	public String getMagazinesUrl() {
		return magazinesUrl;
	}

	public void setMagazinesUrl(String magazinesUrl) {
		this.magazinesUrl = magazinesUrl;
	}

	@ConfigDescription(descriptionKey = "ONLINE_GAMEBASE_URL_DESC", toolTipKey = "ONLINE_GAMEBASE_URL_TOOLTIP")
	private String gamebaseUrl = DEPLOYMENT_URL + "online/gamebase/gb64.jar";

	@Override
	public String getGamebaseUrl() {
		return gamebaseUrl;
	}

	public void setGamebaseUrl(String gamebaseUrl) {
		this.gamebaseUrl = gamebaseUrl;
	}

	@ConfigDescription(descriptionKey = "ONLINE_SOASC_6581R2_DESC", toolTipKey = "ONLINE_SOASC_6581R2_TOOLTIP")
	private String soasc6581R2 = "http://ftp.df.lth.se/pub/media/soasc/soasc_mp3{0}_T{1,number,00}.sid_MOS6581R2.mp3";

	@Override
	public String getSoasc6581R2() {
		return soasc6581R2;
	}

	@Override
	public void setSoasc6581R2(String soasc6581r2) {
		soasc6581R2 = soasc6581r2;
	}

	@ConfigDescription(descriptionKey = "ONLINE_SOASC_6581R4_DESC", toolTipKey = "ONLINE_SOASC_6581R4_TOOLTIP")
	private String soasc6581R4 = "http://ftp.df.lth.se/pub/media/soasc/soasc_mp3{0}_T{1,number,00}.sid_MOS6581R4.mp3";

	@Override
	public String getSoasc6581R4() {
		return soasc6581R4;
	}

	@Override
	public void setSoasc6581R4(String soasc6581r4) {
		soasc6581R4 = soasc6581r4;
	}

	@ConfigDescription(descriptionKey = "ONLINE_SOASC_8580R5_DESC", toolTipKey = "ONLINE_SOASC_8580R5_TOOLTIP")
	private String soasc8580R5 = "http://ftp.df.lth.se/pub/media/soasc/soasc_mp3{0}_T{1,number,00}.sid_CSG8580R5.mp3";

	@Override
	public String getSoasc8580R5() {
		return soasc8580R5;
	}

	@Override
	public void setSoasc8580R5(String soasc8580r5) {
		soasc8580R5 = soasc8580r5;
	}

}
