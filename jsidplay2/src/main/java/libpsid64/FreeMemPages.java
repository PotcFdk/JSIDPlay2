package libpsid64;

class FreeMemPages {
	/**
	 * Start page of driver, 0 means no driver.
	 */
	private Short driverPage;
	/**
	 * Start page of screen, 0 means no screen.
	 */
	private Short screenPage;
	/**
	 * Start page of chars, 0 means no chars.
	 */
	private Short charPage;
	/**
	 * Start page of STIL, 0 means no STIL.
	 */
	private Short stilPage;
	
	public Short getDriverPage() {
		return driverPage;
	}
	
	public void setDriverPage(Short driverPage) {
		this.driverPage = driverPage;
	}
	
	public Short getScreenPage() {
		return screenPage;
	}
	
	public void setScreenPage(Short screenPage) {
		this.screenPage = screenPage;
	}
	
	public Short getCharPage() {
		return charPage;
	}
	
	public void setCharPage(Short charPage) {
		this.charPage = charPage;
	}
	
	public Short getStilPage() {
		return stilPage;
	}
	
	public void setStilPage(Short stilPage) {
		this.stilPage = stilPage;
	}
}