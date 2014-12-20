package de.haendel.service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
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
	public List<File> getDir(@QueryParam("dir") String dir,
			@QueryParam("filter") String filter) {
		List<File> result = new ArrayList<File>();
		result.add(new File(dir, "."));
		result.add(new File(dir, ".."));
		result.addAll(jsidplay2Service.getDirectory(dir, filter));
		return result;
	}

	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path("/download")
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/download?file=/home/ken/Downloads/C64Music/DEMOS/0-9/1_45_Tune.sid
	public Response getDownload(@QueryParam("file") String file) {
		StreamingOutput stream = new StreamingOutput() {

			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				try {
					output.write(Files.readAllBytes(Paths.get(new File(file)
							.getPath())));
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
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path("/convert")
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/convert?file=/home/ken/Downloads/C64Music/DEMOS/0-9/1_45_Tune.sid
	public Response getConvert(@QueryParam("file") String file, @QueryParam("hvsc") String hvsc) {
		try {
			Configuration cfg = new Configuration();
			cfg.getEmulation().setEmulation(Emulation.RESIDFP);
			byte[] convert = jsidplay2Service.convert(cfg, file, hvsc);
			StreamingOutput stream = new StreamingOutput() {
				public void write(OutputStream output) throws IOException,
						WebApplicationException {
					try {
						output.write(convert);
					} catch (Exception e) {
						throw new WebApplicationException(e);
					}
				}
			};
			return Response
					.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
					.header("content-type", "audio/mpeg")
					.header("content-length", convert.length)
					.header("content-disposition",
							"attachment; filename=\""
									+ PathUtils.getBaseNameNoExt(new File(file)
											.getName()) + ".mp3" + "\"")
					.build();
		} catch (InterruptedException e1) {
			return Response.serverError().build();
		} catch (IOException e1) {
			return Response.serverError().build();
		} catch (SidTuneError e1) {
			return Response.serverError().build();
		}
	}

}
