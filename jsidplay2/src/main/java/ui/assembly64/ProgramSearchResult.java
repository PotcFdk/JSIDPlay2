package ui.assembly64;

import java.util.List;

public class ProgramSearchResult {

	private List<ContentEntry> contentEntry;
	private Boolean isContentByItself;

	public ProgramSearchResult() {
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
