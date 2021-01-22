package ui.common.download;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

import javafx.scene.Scene;
import ui.common.UIUtil;
import ui.entities.config.Configuration;

public class MultiDownload {

	private UIUtil util;
	private Configuration cfg;
	private Scene scene;
	private Consumer<File> downloadedFileConsumer;

	public MultiDownload(UIUtil util, Scene scene, Consumer<File> downloadedFileConsumer) {
		this.util = util;
		this.cfg = util.getConfig();
		this.scene = scene;
		this.downloadedFileConsumer = downloadedFileConsumer;
	}

	public void download(List<String> urls) {
		try {
			if (urls.size() == 0) {
				return;
			}
			new DownloadThread(cfg, new ProgressListener(util, scene) {

				@Override
				public void downloaded(final File downloadedFile) {
					if (urls.size() > 1) {
						download(urls.subList(1, urls.size()));
					} else {
						downloadedFileConsumer.accept(downloadedFile);
					}
				}

			}, new URL(urls.get(0)), true).start();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
}
