package ui.musiccollection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.persistence.metamodel.SingularAttribute;

import javafx.util.Pair;
import ui.entities.collection.HVSCEntry;
import ui.entities.collection.HVSCEntry_;
import ui.entities.collection.StilEntry_;

public class SearchCriteria<DECLARING_CLASS, JAVA_TYPE> {
	public SearchCriteria(SingularAttribute<DECLARING_CLASS, JAVA_TYPE> att) {
		this.attribute = att;
	}

	private SingularAttribute<DECLARING_CLASS, JAVA_TYPE> attribute;

	public SingularAttribute<DECLARING_CLASS, JAVA_TYPE> getAttribute() {
		return attribute;
	}

	public static List<SearchCriteria<?, ?>> getSearchableAttributes() {
		List<SearchCriteria<?, ?>> result = new ArrayList<>();
		for (SingularAttribute<? extends Object, ?> singularAttribute : Arrays.asList(HVSCEntry_.path, HVSCEntry_.name,
				HVSCEntry_.title, HVSCEntry_.author, HVSCEntry_.released, HVSCEntry_.format, HVSCEntry_.playerId,
				HVSCEntry_.noOfSongs, HVSCEntry_.startSong, HVSCEntry_.clockFreq, HVSCEntry_.speed,
				HVSCEntry_.sidModel1, HVSCEntry_.sidModel2, HVSCEntry_.sidModel3, HVSCEntry_.compatibility,
				HVSCEntry_.tuneLength, HVSCEntry_.audio, HVSCEntry_.sidChipBase1, HVSCEntry_.sidChipBase2,
				HVSCEntry_.sidChipBase3, HVSCEntry_.driverAddress, HVSCEntry_.loadAddress, HVSCEntry_.loadLength,
				HVSCEntry_.initAddress, HVSCEntry_.playerAddress, HVSCEntry_.fileDate, HVSCEntry_.fileSizeKb,
				HVSCEntry_.tuneSizeB, HVSCEntry_.relocStartPage, HVSCEntry_.relocNoPages, HVSCEntry_.stilGlbComment,
				StilEntry_.stilName, StilEntry_.stilAuthor, StilEntry_.stilTitle, StilEntry_.stilArtist,
				StilEntry_.stilComment)) {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			SearchCriteria<?, ?> criteria = new SearchCriteria(singularAttribute);
			result.add(criteria);
		}
		return result;
	}

	public static List<Pair<String, String>> getAttributeValues(HVSCEntry hvscEntry,
			Function<SearchCriteria<?, ?>, String> nameLocalizer) {
		List<Pair<String, String>> result = new ArrayList<>();
		if (hvscEntry == null) {
			return result;
		}
		for (SearchCriteria<?, ?> field : getSearchableAttributes()) {
			SingularAttribute<?, ?> singleAttribute = field.getAttribute();
			if (!singleAttribute.getDeclaringType().getJavaType().equals(HVSCEntry.class)) {
				continue;
			}
			try {
				String name = nameLocalizer.apply(field);
				Object value = ((Method) singleAttribute.getJavaMember()).invoke(hvscEntry);
				result.add(new Pair<>(name, String.valueOf(value != null ? getText(value) : "")));
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			}
		}
		return result;
	}

	public static String getText(Object value) {
		if (value instanceof Integer && (int) value > 255) {
			return String.format("0x%04X (%d)", value, value);
		}
		if (value instanceof LocalDateTime) {
			return ((LocalDateTime) value).format(DateTimeFormatter.ISO_LOCAL_DATE);
		}
		return value.toString();
	}
}