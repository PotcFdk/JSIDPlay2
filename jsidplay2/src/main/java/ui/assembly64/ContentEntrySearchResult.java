package ui.assembly64;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentEntrySearchResult {

	private List<ContentEntry> contentEntry = new ArrayList<>();
	private Boolean isContentByItself;

	public ContentEntrySearchResult() {
	}

	public List<ContentEntry> getContentEntry() {
		return contentEntry;
	}

	public boolean getIsContentByItself() {
		return isContentByItself;
	}

	public void setIsContentByItself(boolean isContentByItself) {
		this.isContentByItself = isContentByItself;
	}
}
