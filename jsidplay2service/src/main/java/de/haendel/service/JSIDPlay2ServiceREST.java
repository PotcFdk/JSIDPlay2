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
import javax.ws.rs.PathParam;
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
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/directory
	public List<String> getRootDir(@QueryParam("filter") String filter) {
		return jsidplay2Service.getDirectory("/", filter);
	}

	@GET
	@Path("/directory/{filePath : .*}")
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/directory/MUSICIANS
	public List<String> getDir(@PathParam("filePath") String filePath,
			@QueryParam("filter") String filter) {
		return jsidplay2Service.getDirectory(filePath, filter);
	}

	@GET
	@Path("/download/{filePath : .*}")
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/download/DEMOS/0-9/1_45_Tune.sid
	public Response getDownload(@PathParam("filePath") String filePath) {
		StreamingOutput stream = new StreamingOutput() {
			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				try {
					output.write(Files.readAllBytes(Paths.get(jsidplay2Service
							.getFile(filePath).getPath())));
				} catch (Exception e) {
					throw new WebApplicationException(e);
				}
			}
		};
		return Response
				.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
				.header("content-type",
						filePath.endsWith(".mp3") ? "audio/mpeg" : filePath
								.endsWith(".sid") ? "audio/prs.sid" : "bin")
				.header("content-length", new File(filePath).length())
				.header("content-disposition",
						"attachment; filename=\""
								+ new File(filePath).getName() + "\"").build();
	}

	@Produces("audio/mpeg")
	@GET
	@Path("/convert/{filePath : .*}")
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/convert/DEMOS/0-9/1_45_Tune.sid
	public StreamingOutput convert(@PathParam("filePath") String filePath) {
		Configuration cfg = new Configuration();
		cfg.getSidplay2().setDefaultPlayLength(0);
		cfg.getEmulation().setEmulation(Emulation.RESIDFP);
		StreamingOutput stream = new StreamingOutput() {
			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				try {
					jsidplay2Service.convert(cfg, filePath, output);
				} catch (Exception e) {
					throw new WebApplicationException(e);
				}
			}
		};
		return stream;
	}

}
