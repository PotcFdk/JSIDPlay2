package builder.sidblaster;

import java.util.AbstractMap.SimpleEntry;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.EventScheduler;
import libsidplay.config.IConfig;

public class SidBlasterTestBuilder extends SidBlasterBuilder {

	public static String serialNoToTest;

	public SidBlasterTestBuilder(EventScheduler context, IConfig config, CPUClock cpuClock) {
		super(context, config, cpuClock);
	}

	@Override
	protected SimpleEntry<Integer, ChipModel> getModelDependantDeviceId(ChipModel chipModel, int sidNum) {
		Integer deviceId = null;
		for (String serialNo : serialNumbers) {
			if (deviceId == null) {
				deviceId = 0;
			}
			if (serialNo.equals(serialNoToTest)) {
				break;
			}
			deviceId++;
		}
		if (serialNoToTest == null) {
			deviceId = 0;
		}
		return new SimpleEntry<Integer, ChipModel>(deviceId, chipModel);
	}
}