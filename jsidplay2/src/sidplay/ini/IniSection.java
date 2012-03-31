package sidplay.ini;

public abstract class IniSection {

	protected IniReader iniReader;
	
	protected IniSection(IniReader iniReader) {
		this.iniReader = iniReader;
	}
}
