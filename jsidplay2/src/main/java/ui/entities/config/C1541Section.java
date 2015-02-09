package ui.entities.config;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.ExtendImagePolicy;
import sidplay.ini.intf.IC1541Section;

@Embeddable
public class C1541Section implements IC1541Section {

	public static final boolean DEFAULT_DRIVE_SOUND = false;
	public static final ExtendImagePolicy DEFAULT_EXTEND_IMAGE_POLICY = ExtendImagePolicy.EXTEND_ACCESS;

	private BooleanProperty driveOnProperty = new SimpleBooleanProperty(
			DEFAULT_DRIVE_ON);

	@Override
	public boolean isDriveOn() {
		return driveOnProperty.get();
	}

	@Override
	public void setDriveOn(boolean driveOn) {
		driveOnProperty.set(driveOn);
	}

	public BooleanProperty driveOnProperty() {
		return driveOnProperty;
	}

	private BooleanProperty driveSoundOnProperty = new SimpleBooleanProperty(
			DEFAULT_DRIVE_SOUND);

	public boolean isDriveSoundOn() {
		return driveSoundOnProperty.get();
	}

	public void setDriveSoundOn(boolean driveSoundOn) {
		driveSoundOnProperty.set(driveSoundOn);
	}

	public BooleanProperty driveSoundOnProperty() {
		return driveSoundOnProperty;
	}

	private BooleanProperty parallelCableProperty = new SimpleBooleanProperty(
			DEFAULT_PARALLEL_CABLE);

	@Override
	public boolean isParallelCable() {
		return parallelCableProperty.get();
	}

	@Override
	public void setParallelCable(boolean parallelCable) {
		parallelCableProperty.set(parallelCable);
	}

	public BooleanProperty parallelCableProperty() {
		return parallelCableProperty;
	}

	private boolean ramExpansionEnabled0 = DEFAULT_RAM_EXPAND_0X2000;

	@Override
	public boolean isRamExpansionEnabled0() {
		return this.ramExpansionEnabled0;
	}

	@Override
	public void setRamExpansionEnabled0(boolean on) {
		this.ramExpansionEnabled0 = on;
	}

	private boolean ramExpansionEnabled1 = DEFAULT_RAM_EXPAND_0X4000;

	@Override
	public boolean isRamExpansionEnabled1() {
		return this.ramExpansionEnabled1;
	}

	@Override
	public void setRamExpansionEnabled1(boolean on) {
		this.ramExpansionEnabled1 = on;
	}

	private boolean ramExpansionEnabled2 = DEFAULT_RAM_EXPAND_0X6000;

	@Override
	public boolean isRamExpansionEnabled2() {
		return this.ramExpansionEnabled2;
	}

	@Override
	public void setRamExpansionEnabled2(boolean on) {
		this.ramExpansionEnabled2 = on;
	}

	private boolean ramExpansionEnabled3 = DEFAULT_RAM_EXPAND_0X8000;

	@Override
	public boolean isRamExpansionEnabled3() {
		return this.ramExpansionEnabled3;
	}

	@Override
	public void setRamExpansionEnabled3(boolean on) {
		this.ramExpansionEnabled3 = on;
	}

	private boolean ramExpansionEnabled4 = DEFAULT_RAM_EXPAND_0XA000;

	@Override
	public boolean isRamExpansionEnabled4() {
		return this.ramExpansionEnabled4;
	}

	@Override
	public void setRamExpansionEnabled4(boolean on) {
		this.ramExpansionEnabled4 = on;
	}

	private ExtendImagePolicy extendImagePolicy = DEFAULT_EXTEND_IMAGE_POLICY;

	public void setExtendImagePolicy(ExtendImagePolicy policy) {
		this.extendImagePolicy = policy;
	}

	@Enumerated(EnumType.STRING)
	public ExtendImagePolicy getExtendImagePolicy() {
		return this.extendImagePolicy;
	}

	private FloppyType floppyType = DEFAULT_FLOPPY_TYPE;

	@Override
	public void setFloppyType(FloppyType floppyType) {
		this.floppyType = floppyType;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public FloppyType getFloppyType() {
		return floppyType;
	}

}
