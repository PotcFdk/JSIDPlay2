package libsidutils.cruncher;

public class Decruncher {
	private final String resourceName;
	private final String name;
	private final int flags;
	
	public Decruncher(String resourceName, String name, int flags) {
		this.resourceName = resourceName;
		this.name = name;
		this.flags = flags;
	}

	public String getResourceName() {
		return resourceName;
	}
	
	public String getName() {
		return name;
	}

	public int getFlags() {
		return flags;
	}
}
