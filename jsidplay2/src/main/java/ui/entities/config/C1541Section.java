package ui.entities.config;
import static sidplay.ini.IniDefaults.DEFAULT_DRIVE_ON;
import static sidplay.ini.IniDefaults.DEFAULT_FLOPPY_TYPE;
import static sidplay.ini.IniDefaults.DEFAULT_PARALLEL_CABLE;
import static sidplay.ini.IniDefaults.DEFAULT_RAM_EXPAND_0X2000;
import static sidplay.ini.IniDefaults.DEFAULT_RAM_EXPAND_0X4000;
import static sidplay.ini.IniDefaults.DEFAULT_RAM_EXPAND_0X6000;
import static sidplay.ini.IniDefaults.DEFAULT_RAM_EXPAND_0X8000;
import static sidplay.ini.IniDefaults.DEFAULT_RAM_EXPAND_0XA000;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.config.IC1541Section;

@Embeddable
public class C1541Section implements IC1541Section {

	public static final boolean DEFAULT_DRIVE_SOUND = false;
	public static final ExtendImagePolicy DEFAULT_EXTEND_IMAGE_POLICY = ExtendImagePolicy.EXTEND_ACCESS;

	private BooleanProperty driveOnProperty = new SimpleBooleanProperty(DEFAULT_DRIVE_ON);

	public BooleanProperty driveOnProperty() {
		return driveOnProperty;
	}
	
	@Override
	public boolean isDriveOn() {
		return driveOnProperty.get();
	}

	@Override
	public void setDriveOn(boolean driveOn) {
		driveOnProperty.set(driveOn);
	}

	private BooleanProperty driveSoundOnProperty = new SimpleBooleanProperty(DEFAULT_DRIVE_SOUND);

	public BooleanProperty driveSoundOnProperty() {
		return driveSoundOnProperty;
	}
	
	public boolean isDriveSoundOn() {
		return driveSoundOnProperty.get();
	}

	public void setDriveSoundOn(boolean driveSoundOn) {
		driveSoundOnProperty.set(driveSoundOn);
	}

	private BooleanProperty parallelCableProperty = new SimpleBooleanProperty(DEFAULT_PARALLEL_CABLE);

	public BooleanProperty parallelCableProperty() {
		return parallelCableProperty;
	}
	
	@Override
	public boolean isParallelCable() {
		return parallelCableProperty.get();
	}

	@Override
	public void setParallelCable(boolean parallelCable) {
		parallelCableProperty.set(parallelCable);
	}

	private BooleanProperty ramExpansionEnabled0 = new SimpleBooleanProperty(DEFAULT_RAM_EXPAND_0X2000);

	public BooleanProperty ramExpansionEnabled0Property() {
		return ramExpansionEnabled0;
	}
	
	@Override
	public boolean isRamExpansionEnabled0() {
		return this.ramExpansionEnabled0.get();
	}

	@Override
	public void setRamExpansionEnabled0(boolean on) {
		this.ramExpansionEnabled0.set(on);
	}

	private BooleanProperty ramExpansionEnabled1 = new SimpleBooleanProperty(DEFAULT_RAM_EXPAND_0X4000);

	public BooleanProperty ramExpansionEnabled1Property() {
		return ramExpansionEnabled1;
	}
	
	@Override
	public boolean isRamExpansionEnabled1() {
		return this.ramExpansionEnabled1.get();
	}

	@Override
	public void setRamExpansionEnabled1(boolean on) {
		this.ramExpansionEnabled1.set(on);
	}

	private BooleanProperty ramExpansionEnabled2 = new SimpleBooleanProperty(DEFAULT_RAM_EXPAND_0X6000);

	public BooleanProperty ramExpansionEnabled2Property() {
		return ramExpansionEnabled2;
	}
	
	@Override
	public boolean isRamExpansionEnabled2() {
		return this.ramExpansionEnabled2.get();
	}

	@Override
	public void setRamExpansionEnabled2(boolean on) {
		this.ramExpansionEnabled2.set(on);
	}

	private BooleanProperty ramExpansionEnabled3 = new SimpleBooleanProperty(DEFAULT_RAM_EXPAND_0X8000);

	public BooleanProperty ramExpansionEnabled3Property() {
		return ramExpansionEnabled3;
	}
	
	@Override
	public boolean isRamExpansionEnabled3() {
		return this.ramExpansionEnabled3.get();
	}

	@Override
	public void setRamExpansionEnabled3(boolean on) {
		this.ramExpansionEnabled3.set(on);
	}

	private BooleanProperty ramExpansionEnabled4 = new SimpleBooleanProperty(DEFAULT_RAM_EXPAND_0XA000);

	public BooleanProperty ramExpansionEnabled4Property() {
		return ramExpansionEnabled4;
	}
	
	@Override
	public boolean isRamExpansionEnabled4() {
		return this.ramExpansionEnabled4.get();
	}

	@Override
	public void setRamExpansionEnabled4(boolean on) {
		this.ramExpansionEnabled4.set(on);
	}

	private ObjectProperty<ExtendImagePolicy> extendImagePolicy = new SimpleObjectProperty<ExtendImagePolicy>(DEFAULT_EXTEND_IMAGE_POLICY);

	public ObjectProperty<ExtendImagePolicy> extendImagePolicyProperty() {
		return extendImagePolicy;
	}
	
	@Enumerated(EnumType.STRING)
	public ExtendImagePolicy getExtendImagePolicy() {
		return this.extendImagePolicy.get();
	}
	
	public void setExtendImagePolicy(ExtendImagePolicy policy) {
		this.extendImagePolicy.set(policy);
	}

	private ObjectProperty<FloppyType> floppyType = new SimpleObjectProperty<FloppyType>(DEFAULT_FLOPPY_TYPE);

	public ObjectProperty<FloppyType> floppyTypeProperty() {
		return floppyType;
	}
	
	@Enumerated(EnumType.STRING)
	@Override
	public FloppyType getFloppyType() {
		return floppyType.get();
	}
	
	@Override
	public void setFloppyType(FloppyType floppyType) {
		this.floppyType.set(floppyType);
	}

}
