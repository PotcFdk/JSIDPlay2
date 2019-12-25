package ui.musiccollection;

import ui.entities.PersistenceProperties;

public enum MusicCollectionType {
	HVSC("http://www.hvsc.de/", PersistenceProperties.HVSC_DS),
	CGSC("http://www.c64music.co.uk/", PersistenceProperties.CGSC_DS);

	private String url;
	private String dataSource;

	private MusicCollectionType(String url, String dataSource) {
		this.url = url;
		this.dataSource = dataSource;
	}

	public String getUrl() {
		return url;
	}

	public String getDataSource() {
		return dataSource;
	}
}
