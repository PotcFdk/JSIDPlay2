package sidplay.ini;

import sidplay.ini.intf.IOnlineSection;

public class IniOnlineSection extends IniSection implements IOnlineSection {

	public IniOnlineSection(IniReader iniReader) {
		super(iniReader);
	}

	/**
	 * Getter of the download URL for SOASC MOS6581R2.
	 * 
	 * @return the download URL for SOASC MOS6581R2
	 */
	@Override
	public final String getSoasc6581R2() {
		return iniReader.getPropertyString("SIDPlay2", "SOASC_6581R2", null);
	}

	/**
	 * Setter of the download URL for SOASC MOS6581R2.
	 * 
	 * @param soasc6581R2
	 *            the download URL for SOASC MOS6581R2
	 */
	@Override
	public final void setSoasc6581R2(final String soasc6581R2) {
		iniReader.setProperty("SIDPlay2", "SOASC_6581R2", soasc6581R2);
	}

	/**
	 * Getter of the download URL for SOASC MOS6581R4.
	 * 
	 * @return the download URL for SOASC MOS6581R4
	 */
	@Override
	public final String getSoasc6581R4() {
		return iniReader.getPropertyString("SIDPlay2", "SOASC_6581R4", null);
	}

	/**
	 * Setter of the download URL for SOASC MOS6581R4.
	 * 
	 * @param soascr6581R4
	 *            the download URL for SOASC MOS6581R4
	 */
	@Override
	public final void setSoasc6581R4(final String soasc6581R4) {
		iniReader.setProperty("SIDPlay2", "SOASC_6581R2", soasc6581R4);
	}

	/**
	 * Getter of the download URL for SOASC CSG8580R5.
	 * 
	 * @return the download URL for SOASC CSG8580R5
	 */
	@Override
	public final String getSoasc8580R5() {
		return iniReader.getPropertyString("SIDPlay2", "SOASC_8580R5", null);
	}

	/**
	 * Setter of the download URL for SOASC CSG8580R5.
	 * 
	 * @param soasc8580R5
	 *            the download URL for SOASC CSG8580R5
	 */
	@Override
	public final void setSoasc8580R5(final String soasc8580R5) {
		iniReader.setProperty("SIDPlay2", "SOASC_8580R5", soasc8580R5);
	}

	@Override
	public String getHvscUrl() {
		throw new RuntimeException("unsupported feature!");
	}

	@Override
	public String getCgscUrl() {
		throw new RuntimeException("unsupported feature!");
	}

	@Override
	public String getHvmecUrl() {
		throw new RuntimeException("unsupported feature!");
	}

	@Override
	public String getDemosUrl() {
		throw new RuntimeException("unsupported feature!");
	}

	@Override
	public String getMagazinesUrl() {
		throw new RuntimeException("unsupported feature!");
	}

	@Override
	public String getGamebaseUrl() {
		throw new RuntimeException("unsupported feature!");
	}

}
