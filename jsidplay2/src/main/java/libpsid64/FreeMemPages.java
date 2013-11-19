package libpsid64;

class FreeMemPages {
	/**
	 * Start page of driver, 0 means no driver.
	 */
	private short driverPage;
	/**
	 * Start page of screen, 0 means no screen.
	 */
	private short screenPage;
	/**
	 * Start page of chars, 0 means no chars.
	 */
	private short charPage;
	/**
	 * Start page of STIL, 0 means no STIL.
	 */
	private short stilPage;
	
	public short getDriverPage() {
		return driverPage;
	}
	
	public void setDriverPage(short driverPage) {
		this.driverPage = driverPage;
	}
	
	public short getScreenPage() {
		return screenPage;
	}
	
	public void setScreenPage(short screenPage) {
		this.screenPage = screenPage;
	}
	
	public short getCharPage() {
		return charPage;
	}
	
	public void setCharPage(short charPage) {
		this.charPage = charPage;
	}
	
	public short getStilPage() {
		return stilPage;
	}
	
	public void setStilPage(short stilPage) {
		this.stilPage = stilPage;
	}
}