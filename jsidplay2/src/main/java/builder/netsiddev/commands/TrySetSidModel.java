package builder.netsiddev.commands;

import static server.netsiddev.Command.TRY_SET_SID_MODEL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.util.Pair;
import libsidplay.common.ChipModel;

public class TrySetSidModel implements NetSIDPkg {
	private final byte sidNum;
	private final byte config;

	private static Map<Pair<ChipModel, String>, Byte> FILTER_TO_SID_MODEL = new HashMap<>();

	/**
	 * Use SID model of desired filter name and chip model.
	 * 
	 * <B>Note:</B> If filter name is not found use first configuration of
	 * desired chip model. If there is still no match use always the first
	 * available configuration (there must be at least one available).
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param chipModel
	 *            SID chip model
	 * @param filterName
	 *            desired filter name
	 */
	public TrySetSidModel(byte sidNum, ChipModel chipModel, String filterName) {
		this.sidNum = sidNum;
		Optional<Pair<ChipModel, String>> configuration = FILTER_TO_SID_MODEL.keySet().stream()
				.filter(p -> p.getValue().equals(filterName) && p.getKey().equals(chipModel)).findFirst();
		if (configuration.isPresent()) {
			this.config = FILTER_TO_SID_MODEL.get(configuration.get());
		} else {
			System.err.printf("TrySetSidModel: Filter name %s not found!", filterName);
			Optional<String> filter = getFilterNames(chipModel).stream().findFirst();
			if (filter.isPresent()) {
				this.config = FILTER_TO_SID_MODEL.get(new Pair<ChipModel, String>(chipModel, filter.get()));
				System.err.printf("  TrySetSidModel: Use first configuration of chip model=%s, instead!\n", chipModel);
			} else {
				System.err.println("  TrySetSidModel: Use first avalable configuration, instead!");
				this.config = 0;
			}
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
