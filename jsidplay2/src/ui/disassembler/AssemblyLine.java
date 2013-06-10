package ui.disassembler;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AssemblyLine {
	private StringProperty address = new SimpleStringProperty();
	private StringProperty bytes = new SimpleStringProperty();
	private StringProperty mnemonic = new SimpleStringProperty();
	private StringProperty operands = new SimpleStringProperty();
	private StringProperty cycles = new SimpleStringProperty();

	public String getAddress() {
		return address.get();
	}

	public void setAddress(String address) {
		this.address.set(address);
	}

	public String getBytes() {
		return bytes.get();
	}

	public void setBytes(String bytes) {
		this.bytes.set(bytes);
	}

	public String getMnemonic() {
		return mnemonic.get();
	}

	public void setMnemonic(String mnemonic) {
		this.mnemonic.set(mnemonic);
	}

	public String getOperands() {
		return operands.get();
	}

	public void setOperands(String operands) {
		this.operands.set(operands);
	}

	public String getCycles() {
		return cycles.get();
	}

	public void setCycles(String cycles) {
		this.cycles.set(cycles);
	}

}
