package libpsid64;

class FreeMemPages {
	/**
	 * Start page of driver, null means no driver.
	 */
	private Integer driverPage;
	/**
	 * Start page of screen, null means no screen.
	 */
	private Integer screenPage;
	/**
	 * Start page of chars, null means no chars.
	 */
	private Integer charPage;
	/**
	 * Start page of STIL, null means no STIL.
	 */
	private Integer stilPage;
	
	public Integer getDriverPage() {
		return driverPage;
	}
	
	public void setDriverPage(Integer driverPage) {
		this.driverPage = driverPage;
	}
	
	public Integer getScreenPage() {
		return screenPage;
	}
	
	public void setScreenPage(Integer screenPage) {
		this.screenPage = screenPage;
	}
	
	public Integer getCharPage() {
		return charPage;
	}
	
	public void setCharPage(Integer charPage) {
		this.charPage = charPage;
	}
	
	public Integer getStilPage() {
		return stilPage;
	}
	
	public void setStilPage(Integer stilPage) {
		this.stilPage = stilPage;
	}
}