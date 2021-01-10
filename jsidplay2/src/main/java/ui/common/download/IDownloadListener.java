package ui.common.download;

import java.io.File;

public interface IDownloadListener {

	void downloadStep(int step);

	void downloadStop(File downloadedFile);

}
