package ui.entities.config;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

import javax.persistence.Embeddable;

@Embeddable
public class OnlineSection {

	private static final double DEF_ZOOM = 1.5;

	private static final String MAFIFEST_URL_KEY = "url:";

	/**
	 * URL where the JSIDPlay2 is deployed to.
	 */
	private static final String DEPLOYMENT_URL;

	static {
		String codebase = "http://kenchis.t15.org/jsidplay2/javafx/";
		try {
			// Determine download URL from project URL
			URI uri = OnlineSection.class.getResource("/META-INF/MANIFEST.MF").toURI();
			FileSystems.newFileSystem(uri, Collections.singletonMap("create", "true"));
			Optional<String> url = Files.lines(Paths.get(uri)).filter(s -> s.startsWith(MAFIFEST_URL_KEY)).findFirst();
			if (url.isPresent()) {
				codebase = url.get().substring(MAFIFEST_URL_KEY.length()).trim();
			}
		} catch (Exception e) {
			// MANIFEST.MF is only available in a release version!
			e.printStackTrace(System.err);
		}
		DEPLOYMENT_URL = codebase;
	}

	private String hvscUrl = DEPLOYMENT_URL + "C64Music.zip";

	public String getHvscUrl() {
		return hvscUrl;
	}

	public void setHvscUrl(String hvscUrl) {
		this.hvscUrl = hvscUrl;
	}

	private String cgscUrl = DEPLOYMENT_URL + "CGSC.zip";

	public String getCgscUrl() {
		return cgscUrl;
	}

	public void setCgscUrl(String cgscUrl) {
		this.cgscUrl = cgscUrl;
	}

	private String hvmecUrl = DEPLOYMENT_URL + "HVMEC.zip";

	public String getHvmecUrl() {
		return hvmecUrl;
	}

	public void setHvmecUrl(String hvmecUrl) {
		this.hvmecUrl = hvmecUrl;
	}

	private String demosUrl = DEPLOYMENT_URL + "Demos.zip";

	public String getDemosUrl() {
		return demosUrl;
	}

	public void setDemosUrl(String demosUrl) {
		this.demosUrl = demosUrl;
	}

	private String magazinesUrl = DEPLOYMENT_URL + "C64Magazines.zip";

	public String getMagazinesUrl() {
		return magazinesUrl;
	}

	public void setMagazinesUrl(String magazinesUrl) {
		this.magazinesUrl = magazinesUrl;
	}

	private String gamebaseUrl = DEPLOYMENT_URL + "GameBase64.zip";

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

	private double zoom = DEF_ZOOM;

	public double getZoom() {
		return zoom;
	}

	public void setZoom(double zoom) {
		this.zoom = zoom;
	}
}
