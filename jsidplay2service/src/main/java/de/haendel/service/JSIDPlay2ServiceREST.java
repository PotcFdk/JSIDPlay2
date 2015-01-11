package de.haendel.service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.sidtune.SidTuneError;
import ui.entities.config.Configuration;
import de.haendel.impl.IJSIDPlay2;

@Path("/JSIDPlay2REST")
public class JSIDPlay2ServiceREST {

	@Inject
	private IJSIDPlay2 jsidplay2Service;

	@GET
	@Path("/directory")
	@Produces({ "application/json" })
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/directory
	public List<String> getRootDir(@QueryParam("filter") String filter) {
		return jsidplay2Service.getDirectory("/", filter);
	}

	@GET
	@Path("/directory/{filePath : .*}")
	@Produces({ "application/json" })
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/directory/C64Music/MUSICIANS
	public List<String> getDirectory(@PathParam("filePath") String filePath,
			@QueryParam("filter") String filter) {
		return jsidplay2Service.getDirectory(filePath, filter);
	}

	@GET
	@Path("/download/{filePath : .*}")
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/download/C64Music/DEMOS/0-9/1_45_Tune.sid
	public Response getDownload(@PathParam("filePath") String filePath) {
		try {
			byte[] contents = jsidplay2Service.getFile(filePath);
			StreamingOutput stream = new StreamingOutput() {
				public void write(OutputStream output) throws IOException,
						WebApplicationException {
					output.write(contents);
				}
			};
			return Response
					.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
					.header("content-type",
							filePath.endsWith(".mp3") ? "audio/mpeg" : filePath
									.endsWith(".sid") ? "audio/prs.sid" : "bin")
					.header("content-length", contents.length)
					.header("content-disposition",
							"attachment; filename=\""
									+ new File(filePath).getName() + "\"")
					.build();
		} catch (IOException e1) {
			return Response.noContent().build();
		}
	}

	@GET
	@Path("/convert/{filePath : .*}")
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/convert/C64Music/DEMOS/0-9/1_45_Tune.sid
	public Response convert(
			@PathParam("filePath") String filePath,
			@QueryParam("defaultPlayLength") int defaultPlayLength,
			@QueryParam("enableDatabase") boolean enableDatabase,
			@QueryParam("single") boolean single,
			@QueryParam("loop") boolean loop,
			@QueryParam("emulation") Emulation emulation,
			@QueryParam("defaultSidModel") ChipModel defaultSidModel,
			@QueryParam("filter6581") String filter6581,
			@QueryParam("stereoFilter6581") String stereoFilter6581,
			@QueryParam("filter8580") String filter8580,
			@QueryParam("stereoFilter8580") String stereoFilter8580,
			@QueryParam("reSIDfpFilter6581") String reSIDfpFilter6581,
			@QueryParam("reSIDfpStereoFilter6581") String reSIDfpStereoFilter6581,
			@QueryParam("reSIDfpFilter8580") String reSIDfpFilter8580,
			@QueryParam("reSIDfpStereoFilter8580") String reSIDfpStereoFilter8580) {
		Configuration cfg = new Configuration();
		cfg.getSidplay2().setDefaultPlayLength(defaultPlayLength);
		cfg.getSidplay2().setEnableDatabase(enableDatabase);
		cfg.getSidplay2().setSingle(single);
		cfg.getSidplay2().setLoop(loop);
		cfg.getEmulation().setEmulation(emulation);
		cfg.getEmulation().setDefaultSidModel(defaultSidModel);
		cfg.getEmulation().setFilter6581(filter6581);
		cfg.getEmulation().setStereoFilter6581(stereoFilter6581);
		cfg.getEmulation().setFilter8580(filter8580);
		cfg.getEmulation().setStereoFilter8580(stereoFilter8580);
		cfg.getEmulation().setReSIDfpFilter6581(reSIDfpFilter6581);
		cfg.getEmulation().setReSIDfpStereoFilter6581(reSIDfpStereoFilter6581);
		cfg.getEmulation().setReSIDfpFilter8580(reSIDfpFilter8580);
		cfg.getEmulation().setReSIDfpStereoFilter8580(reSIDfpStereoFilter8580);
		StreamingOutput stream = new StreamingOutput() {
			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				try {
					jsidplay2Service.convert(cfg, filePath, output);
				} catch (InterruptedException e) {
					throw new WebApplicationException(e);
				} catch (SidTuneError e) {
					throw new WebApplicationException(e);
				}
			}
		};
		return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
				.header("content-type", "audio/mpeg").build();
	}

	@GET
	@Path("/photo/{filePath : .*}")
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/photo/C64Music/DEMOS/0-9/1_45_Tune.sid
	public byte[] getPhoto(@PathParam("filePath") String filePath) {
		try {
			return jsidplay2Service.getPhoto(filePath);
		} catch (IOException e) {
			throw new WebApplicationException(e);
		} catch (SidTuneError e) {
			throw new WebApplicationException(e);
		}
	}

	@GET
	@Path("/info/{filePath : .*}")
	@Produces({ "application/json" })
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/info/C64Music/DEMOS/0-9/1_45_Tune.sid
	public Map<String, String> getTuneInfos(
			@PathParam("filePath") String filePath) {
		try {
			return jsidplay2Service.getTuneInfos(filePath);
		} catch (IOException e) {
			throw new WebApplicationException(e);
		} catch (SidTuneError e) {
			throw new WebApplicationException(e);
		}
	}

	@GET
	@Path("/filters")
	@Produces({ "application/json" })
	public List<String> getFilters() {
		return jsidplay2Service.getFilters();
	}
}
