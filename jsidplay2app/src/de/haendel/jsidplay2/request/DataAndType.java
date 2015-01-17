package de.haendel.jsidplay2.request;

import android.net.Uri;

public class DataAndType {
	private Uri uri;
	private String type;
	
	public Uri getUri() {
		return uri;
	}
	
	public void setUri(Uri uri) {
		this.uri = uri;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
}