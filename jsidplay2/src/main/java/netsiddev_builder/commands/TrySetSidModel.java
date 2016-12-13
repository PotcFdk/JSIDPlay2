package netsiddev_builder.commands;

import static netsiddev.Command.TRY_SET_SID_MODEL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.util.Pair;
import libsidplay.common.ChipModel;

public class TrySetSidModel implements NetSIDPkg {
	private byte sidNum;
	private byte config;

	private static Map<Pair<ChipModel, String>, Byte> FILTER_TO_SID_MODEL = new HashMap<>();

	public TrySetSidModel(byte sidNum, ChipModel chipModel, String filterName) {
		this.sidNum = sidNum;
		final Optional<Pair<ChipModel, String>> configuration = FILTER_TO_SID_MODEL.keySet().stream()
				.filter(p -> p.getKey().equals(chipModel) && p.getValue().equals(filterName)).findFirst();
		if (configuration.isPresent()) {
			this.config = FILTER_TO_SID_MODEL.get(configuration.get());
		} else {
			System.err.println("TrySetSidModel: Use default config for filter name=" + filterName);
		}
	}

	public TrySetSidModel(byte sidNum, byte config) {
		this.sidNum = sidNum;
		this.config = config;
	}

	public static Map<Pair<ChipModel, String>, Byte> getFilterToSidModel() {
		return FILTER_TO_SID_MODEL;
	}

	/**
	 * @param model
	 *            chip model
	 * @return sorted filter names of the desired chip model (case-insensitive)
	 */
	public static List<String> getFilterNames(ChipModel model) {
		return FILTER_TO_SID_MODEL.keySet().stream().filter(p -> p.getKey() == model).map(p -> p.getValue())
				.sorted((s1, s2) -> s1.compareToIgnoreCase(s2)).collect(Collectors.toList());
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) TRY_SET_SID_MODEL.ordinal(), sidNum, 0, 0, config };
	}

}
