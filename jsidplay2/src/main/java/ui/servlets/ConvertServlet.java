package ui.servlets;

import static sidplay.ini.IniDefaults.DEFAULT_APP_SERVER_DIR;
import static ui.servlets.JSIDPlay2Server.MIME_TYPE_MPEG;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.config.IConfig;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.siddatabase.SidDatabase;
import sidplay.Player;
import sidplay.audio.AudioDriver;
import sidplay.audio.MP3Driver.MP3Stream;
import sidplay.ini.IniDefaults;

public class ConvertServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String HVSC_ROOT = DEFAULT_APP_SERVER_DIR + "/C64Music";
	public static final String SERVLET_PATH = "/convert";

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String filePath = request.getRequestURI()
				.substring(request.getRequestURI().indexOf(SERVLET_PATH) + SERVLET_PATH.length());

		IConfig cfg = IniDefaults.DEFAULTS;
		cfg.getSidplay2Section().setDefaultPlayLength(Integer.parseInt(request.getParameter("defaultPlayLength")));
		cfg.getSidplay2Section().setEnableDatabase(Boolean.parseBoolean(request.getParameter("enableDatabase")));
		cfg.getSidplay2Section().setSingle(Boolean.parseBoolean(request.getParameter("single")));
		cfg.getSidplay2Section().setLoop(Boolean.parseBoolean(request.getParameter("loop")));
		cfg.getSidplay2Section().setFadeInTime(5);
		cfg.getSidplay2Section().setFadeOutTime(5);
		cfg.getAudioSection().setBufferSize(Integer.parseInt(request.getParameter("bufferSize")));
		cfg.getAudioSection().setSampling(SamplingMethod.valueOf(request.getParameter("samplingMethod")));
		SamplingRate sampligRate = Integer.parseInt(request.getParameter("frequency")) == 44100 ? SamplingRate.LOW
				: Integer.parseInt(request.getParameter("frequency")) == 48000 ? SamplingRate.MEDIUM
						: SamplingRate.HIGH;
		cfg.getAudioSection().setSamplingRate(sampligRate);
		cfg.getAudioSection().setMainVolume(3f);
		cfg.getAudioSection().setSecondVolume(3f);
		cfg.getAudioSection().setThirdVolume(3f);
		cfg.getAudioSection().setMainBalance(0.5f);
		cfg.getAudioSection().setSecondBalance(0.5f);
		cfg.getAudioSection().setThirdBalance(0.5f);
		cfg.getEmulationSection().setDefaultEmulation(Emulation.valueOf(request.getParameter("emulation")));
		cfg.getEmulationSection().setDefaultSidModel(ChipModel.valueOf(request.getParameter("defaultSidModel")));
		cfg.getEmulationSection().setFilter6581(request.getParameter("filter6581"));
		cfg.getEmulationSection().setStereoFilter6581(request.getParameter("stereoFilter6581"));
		cfg.getEmulationSection().setThirdSIDFilter6581(request.getParameter("thirdFilter6581"));
		cfg.getEmulationSection().setFilter8580(request.getParameter("filter8580"));
		cfg.getEmulationSection().setStereoFilter8580(request.getParameter("stereoFilter8580"));
		cfg.getEmulationSection().setThirdSIDFilter8580(request.getParameter("thirdFilter8580"));
		cfg.getEmulationSection().setReSIDfpFilter6581(request.getParameter("reSIDfpFilter6581"));
		cfg.getEmulationSection().setReSIDfpStereoFilter6581(request.getParameter("reSIDfpStereoFilter6581"));
		cfg.getEmulationSection().setReSIDfpThirdSIDFilter6581(request.getParameter("reSIDfpThirdFilter6581"));
		cfg.getEmulationSection().setReSIDfpFilter8580(request.getParameter("reSIDfpFilter8580"));
		cfg.getEmulationSection().setReSIDfpStereoFilter8580(request.getParameter("reSIDfpStereoFilter8580"));
		cfg.getEmulationSection().setReSIDfpThirdSIDFilter8580(request.getParameter("reSIDfpThirdFilter8580"));
		cfg.getEmulationSection().setDigiBoosted8580(Boolean.parseBoolean(request.getParameter("digiBoosted8580")));

		response.setContentType(MIME_TYPE_MPEG);
		try (ServletOutputStream out = response.getOutputStream()) {
			MP3Stream driver = new MP3Stream(out);
			driver.setCbr(Integer.parseInt(request.getParameter("cbr")));
			driver.setVbrQuality(Integer.parseInt(request.getParameter("vbr")));
			driver.setVbr(Boolean.parseBoolean(request.getParameter("isVbr")));
			convert(cfg, filePath, driver);
		} catch (SidTuneError e) {
			throw new ServletException(e.getMessage());
		} finally {
			response.getOutputStream().flush();
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	public void convert(IConfig config, String resource, AudioDriver driver) throws IOException, SidTuneError {
		Player player = new Player(config);
		player.setSidDatabase(new SidDatabase(HVSC_ROOT));
		player.setAudioDriver(driver);
		player.play(SidTune.load(getAbsoluteFile(resource)));
		player.stopC64(false);
	}

	private File getAbsoluteFile(String path) {
		return new File(DEFAULT_APP_SERVER_DIR, path);
	}
}
