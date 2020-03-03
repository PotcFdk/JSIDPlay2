package ui.sidreg;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SidRegWrite {
	private LongProperty absCycles = new SimpleLongProperty();
	private LongProperty relCycles = new SimpleLongProperty();;
	private IntegerProperty address = new SimpleIntegerProperty();
	private StringProperty description = new SimpleStringProperty();
	private IntegerProperty value = new SimpleIntegerProperty();

	public SidRegWrite() {
	}

	public SidRegWrite(Long absCycles, Long relCycles, Integer addr, String description, Integer value) {
		setAbsCycles(absCycles);
		setRelCycles(relCycles);
		setAddress(addr);
		setDescription(description);
		setValue(value);
	}

	public Long getAbsCycles() {
		return absCycles.get();
	}

	public void setAbsCycles(Long value) {
		absCycles.set(value);
	}

	public Long getRelCycles() {
		return relCycles.get();
	}

	public void setRelCycles(Long value) {
		relCycles.set(value);
	}

	public Integer getAddress() {
		return address.get();
	}

	public void setAddress(Integer value) {
		address.set(value);
	}

	public String getHexAddress() {
		return String.format("$%04X", address.get());
	}

	public String getDescription() {
		return description.get();
	}

	public void setDescription(String value) {
		description.set(value);
	}

	public Integer getValue() {
		return value.get();
	}

	public void setValue(Integer value) {
		this.value.set(value);
	}

	public String getHexValue() {
		return String.format("$%02X", value.get());
	}
}
