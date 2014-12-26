package de.haendel.service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import libsidplay.common.Emulation;
import ui.entities.config.Configuration;
import de.haendel.impl.IJSIDPlay2;

@Path("/JSIDPlay2REST")
@Consumes({ "application/json" })
@Produces({ "application/json" })
public class JSIDPlay2ServiceREST {

	@Inject
	private IJSIDPlay2 jsidplay2Service;

	@GET
	@Path("/directory")
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/directory?dir=/media
	public List<String> getDir(@QueryParam("dir") String dir,
			@QueryParam("filter") String filter) {
		return jsidplay2Service.getDirectory(dir, filter);
	}

	@GET
	@Path("/download")
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/download?file=/home/ken/Downloads/C64Music/DEMOS/0-9/1_45_Tune.sid
	public Response getDownload(@QueryParam("file") String file) {
		StreamingOutput stream = new StreamingOutput() {

			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				try {
					output.write(Files.readAllBytes(Paths.get(jsidplay2Service
							.getFile(file).getPath())));
				} catch (Exception e) {
					throw new WebApplicationException(e);
				}
			}
		};
		return Response
				.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
				.header("content-type",
						file.endsWith(".mp3") ? "audio/mpeg" : file
								.endsWith(".sid") ? "audio/prs.sid" : "bin")
				.header("content-length", new File(file).length())
				.header("content-disposition",
						"attachment; filename=\"" + new File(file).getName()
								+ "\"").build();
	}

	@GET
	@Produces("audio/mpeg")
	@Path("/convert")
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/convert?file=/home/ken/Downloads/C64Music/DEMOS/0-9/1_45_Tune.sid
	public StreamingOutput getConvert(@QueryParam("file") String file) {
		Configuration cfg = new Configuration();
		cfg.getSidplay2().setDefaultPlayLength(0);
		cfg.getEmulation().setEmulation(Emulation.RESIDFP);
		StreamingOutput stream = new StreamingOutput() {
			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				try {
					jsidplay2Service.convert(cfg, file, output);
				} catch (Exception e) {
					throw new WebApplicationException(e);
				}
			}
		};
		return stream;
	}

}
