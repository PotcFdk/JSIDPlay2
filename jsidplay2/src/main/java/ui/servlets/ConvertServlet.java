package ui.servlets;

import static ui.servlets.JSIDPlay2Server.MIME_TYPE_MPEG;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Locale;
import java.util.stream.Stream;

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
import ui.entities.config.Configuration;

public class ConvertServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH_CONVERT = "/convert";

	private ServletUtil util;

	public ConvertServlet(Configuration configuration) {
		this.util = new ServletUtil(configuration);
	}

	/**
	 * Stream SID as MP3.
	 * 
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/convert/C64Music/DEMOS/0-9/1_45_Tune.sid
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String filePath = java.net.URLDecoder.decode(request.getRequestURI(), "UTF-8")
				.substring(request.getRequestURI().indexOf(SERVLET_PATH_CONVERT) + SERVLET_PATH_CONVERT.length());

		if (filePath.toLowerCase(Locale.ENGLISH).endsWith(".mp3")) {
			response.setContentType(MIME_TYPE_MPEG);
			response.addHeader("Content-Disposition", "attachment; filename=" + new File(filePath).getName());

			try (ServletOutputStream servletOutputStream = response.getOutputStream()) {
				servletOutputStream.write(Files
						.readAllBytes(Paths.get(util.getAbsoluteFile(filePath, request.getUserPrincipal()).getPath())));
			} finally {
				response.getOutputStream().flush();
			}
			response.setStatus(HttpServletResponse.SC_OK);
		} else {
			// we use default base settings to not disturb the main player instance
			IConfig cfg = IniDefaults.DEFAULTS;
			cfg.getSidplay2Section().setDefaultPlayLength(Integer.parseInt(request.getParameter("defaultPlayLength")));
			cfg.getSidplay2Section().setEnableDatabase(Boolean.parseBoolean(request.getParameter("enableDatabase")));
			cfg.getSidplay2Section().setSingle(Boolean.parseBoolean(request.getParameter("single")));
			cfg.getSidplay2Section().setLoop(Boolean.parseBoolean(request.getParameter("loop")));
			cfg.getSidplay2Section().setFadeInTime(5);
			cfg.getSidplay2Section().setFadeOutTime(5);
			cfg.getAudioSection().setBufferSize(Integer.parseInt(request.getParameter("bufferSize")));
			cfg.getAudioSection().setSampling(SamplingMethod.valueOf(request.getParameter("samplingMethod")));
			Stream.of(SamplingRate.values())
					.filter(rate -> rate.getFrequency() == Integer.parseInt(request.getParameter("frequency")))
					.findFirst().ifPresent(freq -> cfg.getAudioSection().setSamplingRate(freq));
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
				convert(cfg, filePath, driver, request.getUserPrincipal());
			} catch (SidTuneError e) {
				throw new ServletException(e.getMessage());
			} finally {
				response.getOutputStream().flush();
			}
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}

	private void convert(IConfig config, String resource, AudioDriver driver, Principal principal)
			throws IOException, SidTuneError {
		Player player = new Player(config);
		String root = util.getConfiguration().getSidplay2Section().getHvsc();
		if (root != null) {
			player.setSidDatabase(new SidDatabase(root));
		}
		player.setAudioDriver(driver);
		player.play(SidTune.load(util.getAbsoluteFile(resource, principal)));
		player.stopC64(false);
	}

}
