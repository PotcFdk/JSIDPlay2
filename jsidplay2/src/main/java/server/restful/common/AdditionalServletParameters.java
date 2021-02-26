package server.restful.common;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "server.restful.common.AdditionalServletParameters")
public class AdditionalServletParameters {

	@Parameter(names = "--download", arity = 1, descriptionKey = "DOWNLOAD")
	private Boolean download = Boolean.FALSE;

	public Boolean getDownload() {
		return download;
	}
}
