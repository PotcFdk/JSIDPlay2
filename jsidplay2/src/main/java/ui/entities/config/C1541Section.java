package ui.entities.config;

import static sidplay.ini.IniDefaults.DEFAULT_DRIVE_ON;
import static sidplay.ini.IniDefaults.DEFAULT_FLOPPY_TYPE;
import static sidplay.ini.IniDefaults.DEFAULT_JIFFYDOS_INSTALLED;
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
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.FloppyType;
import libsidplay.config.IC1541Section;
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.properties.ShadowField;

@Embeddable
public class C1541Section implements IC1541Section {

	public static final boolean DEFAULT_DRIVE_SOUND = false;
	public static final ExtendImagePolicy DEFAULT_EXTEND_IMAGE_POLICY = ExtendImagePolicy.EXTEND_ACCESS;

	private ShadowField<BooleanProperty, Boolean> driveOn = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_DRIVE_ON);

	@Override
	public final boolean isDriveOn() {
		return driveOn.get();
	}

	@Override
	public final void setDriveOn(boolean driveOn) {
		this.driveOn.set(driveOn);
	}

	public final BooleanProperty driveOnProperty() {
		return driveOn.property();
	}

	private ShadowField<BooleanProperty, Boolean> driveSoundOn = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_DRIVE_SOUND);

	public final boolean isDriveSoundOn() {
		return driveSoundOn.get();
	}

	public final void setDriveSoundOn(boolean driveSoundOn) {
		this.driveSoundOn.set(driveSoundOn);
	}

	public final BooleanProperty driveSoundOnProperty() {
		return driveSoundOn.property();
	}

	private ShadowField<BooleanProperty, Boolean> parallelCable = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_PARALLEL_CABLE);

	@Override
	public final boolean isParallelCable() {
		return parallelCable.get();
	}

	@Override
	public final void setParallelCable(boolean parallelCable) {
		this.parallelCable.set(parallelCable);
	}

	public final BooleanProperty parallelCableProperty() {
		return parallelCable.property();
	}

	private ShadowField<BooleanProperty, Boolean> jiffyDosInstalled = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_JIFFYDOS_INSTALLED);

	@Override
	public final boolean isJiffyDosInstalled() {
		return this.jiffyDosInstalled.get();
	}

	@Override
	public final void setJiffyDosInstalled(boolean on) {
		this.jiffyDosInstalled.set(on);
	}

	public final BooleanProperty jiffyDosInstalledProperty() {
		return jiffyDosInstalled.property();
	}

	private ShadowField<BooleanProperty, Boolean> ramExpansionEnabled0 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_RAM_EXPAND_0X2000);

	@Override
	public final boolean isRamExpansionEnabled0() {
		return ramExpansionEnabled0.get();
	}

	@Override
	public final void setRamExpansionEnabled0(boolean on) {
		this.ramExpansionEnabled0.set(on);
	}

	public final BooleanProperty ramExpansionEnabled0Property() {
		return ramExpansionEnabled0.property();
	}

	private ShadowField<BooleanProperty, Boolean> ramExpansionEnabled1 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_RAM_EXPAND_0X4000);

	@Override
	public final boolean isRamExpansionEnabled1() {
		return ramExpansionEnabled1.get();
	}

	@Override
	public final void setRamExpansionEnabled1(boolean on) {
		this.ramExpansionEnabled1.set(on);
	}

	public final BooleanProperty ramExpansionEnabled1Property() {
		return ramExpansionEnabled1.property();
	}

	private ShadowField<BooleanProperty, Boolean> ramExpansionEnabled2 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_RAM_EXPAND_0X6000);

	@Override
	public final boolean isRamExpansionEnabled2() {
		return ramExpansionEnabled2.get();
	}

	@Override
	public final void setRamExpansionEnabled2(boolean on) {
		this.ramExpansionEnabled2.set(on);
	}

	public final BooleanProperty ramExpansionEnabled2Property() {
		return ramExpansionEnabled2.property();
	}

	private ShadowField<BooleanProperty, Boolean> ramExpansionEnabled3 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_RAM_EXPAND_0X8000);

	@Override
	public final boolean isRamExpansionEnabled3() {
		return ramExpansionEnabled3.get();
	}

	@Override
	public final void setRamExpansionEnabled3(boolean on) {
		this.ramExpansionEnabled3.set(on);
	}

	public final BooleanProperty ramExpansionEnabled3Property() {
		return ramExpansionEnabled3.property();
	}

	private ShadowField<BooleanProperty, Boolean> ramExpansionEnabled4 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_RAM_EXPAND_0XA000);

	@Override
	public final boolean isRamExpansionEnabled4() {
		return ramExpansionEnabled4.get();
	}

	@Override
	public final void setRamExpansionEnabled4(boolean on) {
		this.ramExpansionEnabled4.set(on);
	}

	public final BooleanProperty ramExpansionEnabled4Property() {
		return ramExpansionEnabled4.property();
	}

	private ShadowField<ObjectProperty<ExtendImagePolicy>, ExtendImagePolicy> extendImagePolicy = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_EXTEND_IMAGE_POLICY);

	@Enumerated(EnumType.STRING)
	public final ExtendImagePolicy getExtendImagePolicy() {
		return extendImagePolicy.get();
	}

	public final void setExtendImagePolicy(ExtendImagePolicy policy) {
		this.extendImagePolicy.set(policy);
	}

	public final ObjectProperty<ExtendImagePolicy> extendImagePolicyProperty() {
		return extendImagePolicy.property();
	}

	private ShadowField<ObjectProperty<FloppyType>, FloppyType> floppyType = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_FLOPPY_TYPE);

	@Enumerated(EnumType.STRING)
	@Override
	public final FloppyType getFloppyType() {
		return floppyType.get();
	}

	@Override
	public final void setFloppyType(FloppyType floppyType) {
		this.floppyType.set(floppyType);
	}

	public final ObjectProperty<FloppyType> floppyTypeProperty() {
		return floppyType.property();
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
