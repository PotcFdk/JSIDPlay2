package applet.entities.config;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.ExtendImagePolicy;
import sidplay.ini.intf.IC1541Section;
import applet.config.annotations.ConfigDescription;

@Embeddable
public class C1541Section implements IC1541Section {

	@ConfigDescription(descriptionKey = "1541_DRIVE_ON_DESC", toolTipKey = "1541_DRIVE_ON_TOOLTIP")
	private boolean driveOn;

	@Override
	public boolean isDriveOn() {
		return this.driveOn;
	}

	@Override
	public void setDriveOn(boolean on) {
		this.driveOn = on;
	}

	@ConfigDescription(descriptionKey = "1541_DRIVE_SOUND_ON_DESC", toolTipKey = "1541_DRIVE_SOUND_ON_TOOLTIP")
	private boolean driveSoundOn = true;

	@Override
	public boolean isDriveSoundOn() {
		return this.driveSoundOn;
	}

	@Override
	public void setDriveSoundOn(boolean on) {
		this.driveSoundOn = on;
	}

	@ConfigDescription(descriptionKey = "1541_PARALLEL_CABLE_DESC", toolTipKey = "1541_PARALLEL_CABLE_TOOLTIP")
	private boolean parallelCable;

	@Override
	public boolean isParallelCable() {
		return this.parallelCable;
	}

	@Override
	public void setParallelCable(boolean on) {
		this.parallelCable = on;
	}

	@ConfigDescription(descriptionKey = "1541_RAM_EXPANSION_ENABLED_0_DESC", toolTipKey = "1541_RAM_EXPANSION_ENABLED_0_TOOLTIP")
	private boolean ramExpansionEnabled0;

	@Override
	public boolean isRamExpansionEnabled0() {
		return this.ramExpansionEnabled0;
	}

	@Override
	public void setRamExpansion0(boolean on) {
		this.ramExpansionEnabled0 = on;
	}

	@ConfigDescription(descriptionKey = "1541_RAM_EXPANSION_ENABLED_1_DESC", toolTipKey = "1541_RAM_EXPANSION_ENABLED_1_TOOLTIP")
	private boolean ramExpansionEnabled1;

	@Override
	public boolean isRamExpansionEnabled1() {
		return this.ramExpansionEnabled1;
	}

	@Override
	public void setRamExpansion1(boolean on) {
		this.ramExpansionEnabled1 = on;
	}

	@ConfigDescription(descriptionKey = "1541_RAM_EXPANSION_ENABLED_2_DESC", toolTipKey = "1541_RAM_EXPANSION_ENABLED_2_TOOLTIP")
	private boolean ramExpansionEnabled2;

	@Override
	public boolean isRamExpansionEnabled2() {
		return this.ramExpansionEnabled2;
	}

	@Override
	public void setRamExpansion2(boolean on) {
		this.ramExpansionEnabled2 = on;
	}

	@ConfigDescription(descriptionKey = "1541_RAM_EXPANSION_ENABLED_3_DESC", toolTipKey = "1541_RAM_EXPANSION_ENABLED_3_TOOLTIP")
	private boolean ramExpansionEnabled3;

	@Override
	public boolean isRamExpansionEnabled3() {
		return this.ramExpansionEnabled3;
	}

	@Override
	public void setRamExpansion3(boolean on) {
		this.ramExpansionEnabled3 = on;
	}

	@ConfigDescription(descriptionKey = "1541_RAM_EXPANSION_ENABLED_4_DESC", toolTipKey = "1541_RAM_EXPANSION_ENABLED_4_TOOLTIP")
	private boolean ramExpansionEnabled4;

	@Override
	public boolean isRamExpansionEnabled4() {
		return this.ramExpansionEnabled4;
	}

	@Override
	public void setRamExpansion4(boolean on) {
		this.ramExpansionEnabled4 = on;
	}

	@ConfigDescription(descriptionKey = "1541_EXTEND_IMAGE_POLICY_DESC", toolTipKey = "1541_EXTEND_IMAGE_POLICY_TOOLTIP")
	@Enumerated(EnumType.STRING)
	private ExtendImagePolicy extendImagePolicy = ExtendImagePolicy.EXTEND_ACCESS;

	@Override
	public void setExtendImagePolicy(ExtendImagePolicy policy) {
		this.extendImagePolicy = policy;
	}

	@Override
	public ExtendImagePolicy getExtendImagePolicy() {
		return this.extendImagePolicy;
	}

	@Enumerated(EnumType.STRING)
	@ConfigDescription(descriptionKey = "1541_FLOPPY_TYPE_DESC", toolTipKey = "1541_FLOPPY_TYPE_TOOLTIP")
	private FloppyType floppyType = FloppyType.C1541;

	@Override
	public void setFloppyType(FloppyType floppyType) {
		this.floppyType = floppyType;
	}

	@Override
	public FloppyType getFloppyType() {
		return floppyType;
	}

}
