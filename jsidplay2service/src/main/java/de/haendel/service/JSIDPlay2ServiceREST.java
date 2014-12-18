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
import libsidutils.PathUtils;
import ui.entities.config.Configuration;
import de.haendel.impl.IJSIDPlay2;

@Path("/JSIDPlay2")
@Consumes({ "application/json" })
@Produces({ "application/json" })
public class JSIDPlay2ServiceREST {

	private static final String ROOT = "/media/readyshare/Musik";
	private static final String ROOTSID = "/home/ken/Downloads/C64Music";

	@Inject
	private IJSIDPlay2 jsidplay2Service;

	@GET
	@Path("/root")
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2/root
	public List<File> getRoot() {
		List<File> result = new ArrayList<File>();
		result.addAll(jsidplay2Service.getDirectory(ROOT));
		result.addAll(jsidplay2Service.getDirectory(ROOTSID));
		return result;
	}

	@GET
	@Path("/root/directory")
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2/root/directory?dir=/media
	public List<File> getDir(@QueryParam("dir") String dir) {
		return jsidplay2Service.getDirectory(dir);
	}

	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path("/root/download")
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2/root/download?file=/home/ken/Downloads/C64Music/DEMOS/0-9/1_45_Tune.sid
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
				.header("content-disposition",
						"attachment; filename=\"" + new File(file).getName()
								+ "\"").build();
	}

	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path("/root/convert")
	// http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2/root/convert?file=/home/ken/Downloads/C64Music/DEMOS/0-9/1_45_Tune.sid
	public Response getConvert(@QueryParam("file") String file) {
		StreamingOutput stream = new StreamingOutput() {

			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				try {
					Configuration cfg = new Configuration();
					cfg.getEmulation().setEmulation(Emulation.RESIDFP);
					output.write(jsidplay2Service.convert(cfg, file));
				} catch (Exception e) {
					throw new WebApplicationException(e);
				}
			}
		};
		return Response
				.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
				.header("content-type", "audio/mpeg")
				.header("content-disposition",
						"attachment; filename=\""
								+ PathUtils.getBaseNameNoExt(new File(file)
										.getName()) + ".mp3" + "\"").build();
	}

}
