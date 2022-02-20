package server.restful.common;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import sidplay.ini.IniConfig;

@Parameters(resourceBundle = "server.restful.common.ServletParameters")
public class ServletParameters {

	@Parameter(names = { "--startSong" }, descriptionKey = "START_SONG")
	private Integer song = null;

	@Parameter(names = "--download", arity = 1, descriptionKey = "DOWNLOAD")
	private Boolean download = Boolean.FALSE;

	@ParametersDelegate
	private IniConfig config = new IniConfig();

	private volatile boolean started;

	public Integer getSong() {
		return song;
	}

	public Boolean getDownload() {
		return download;
	}

	public IniConfig getConfig() {
		return config;
	}

	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}
}
