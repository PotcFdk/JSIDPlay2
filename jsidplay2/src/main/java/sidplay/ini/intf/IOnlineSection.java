package sidplay.ini.intf;

public interface IOnlineSection {

	String getHvscUrl();

	String getCgscUrl();

	String getHvmecUrl();

	String getDemosUrl();

	String getMagazinesUrl();

	String getGamebaseUrl();

	/**
	 * Getter of the download URL for SOASC MOS6581R2.
	 * 
	 * @return the download URL for SOASC MOS6581R2
	 */
	String getSoasc6581R2();

	/**
	 * Setter of the download URL for SOASC MOS6581R2.
	 * 
	 * @param soasc6581R2
	 *            the download URL for SOASC MOS6581R2
	 */
	void setSoasc6581R2(String soasc6581R2);

	/**
	 * Getter of the download URL for SOASC MOS6581R4.
	 * 
	 * @return the download URL for SOASC MOS6581R4
	 */
	String getSoasc6581R4();

	/**
	 * Setter of the download URL for SOASC MOS6581R4.
	 * 
	 * @param soasc6581R4
	 *            the download URL for SOASC MOS6581R4
	 */
	void setSoasc6581R4(String soasc6581R4);

	/**
	 * Getter of the download URL for SOASC CSG8580R5.
	 * 
	 * @return the download URL for SOASC CSG8580R5
	 */
	String getSoasc8580R5();

	/**
	 * Setter of the download URL for SOASC CSG8580R5.
	 * 
	 * @param soasc8580R5
	 *            the download URL for SOASC CSG8580R5
	 */
	void setSoasc8580R5(String soasc8580R5);

}
