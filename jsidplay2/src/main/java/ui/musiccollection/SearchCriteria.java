package ui.musiccollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.metamodel.SingularAttribute;

import ui.entities.collection.HVSCEntry_;
import ui.entities.collection.StilEntry_;

class SearchCriteria<DECLARING_CLASS, JAVA_TYPE> {
	public SearchCriteria(SingularAttribute<DECLARING_CLASS, JAVA_TYPE> att) {
		this.attribute = att;
	}

	private SingularAttribute<DECLARING_CLASS, JAVA_TYPE> attribute;

	public SingularAttribute<DECLARING_CLASS, JAVA_TYPE> getAttribute() {
		return attribute;
	}

	public static List<SearchCriteria<?, ?>> getSearchableAttributes() {
		List<SearchCriteria<?, ?>> result = new ArrayList<SearchCriteria<?, ?>>();
		for (SingularAttribute<? extends Object, ?> singularAttribute : Arrays
				.asList(HVSCEntry_.path, HVSCEntry_.name, HVSCEntry_.title,
						HVSCEntry_.author, HVSCEntry_.released,
						HVSCEntry_.format, HVSCEntry_.playerId,
						HVSCEntry_.noOfSongs, HVSCEntry_.startSong,
						HVSCEntry_.clockFreq, HVSCEntry_.speed,
						HVSCEntry_.sidModel1, HVSCEntry_.sidModel2,
						HVSCEntry_.compatibility, HVSCEntry_.tuneLength,
						HVSCEntry_.audio, HVSCEntry_.sidChipBase1,
						HVSCEntry_.sidChipBase2, HVSCEntry_.driverAddress,
						HVSCEntry_.loadAddress, HVSCEntry_.loadLength,
						HVSCEntry_.initAddress, HVSCEntry_.playerAddress,
						HVSCEntry_.fileDate, HVSCEntry_.fileSizeKb,
						HVSCEntry_.tuneSizeB, HVSCEntry_.relocStartPage,
						HVSCEntry_.relocNoPages, HVSCEntry_.stilGlbComment,
						StilEntry_.stilName, StilEntry_.stilAuthor,
						StilEntry_.stilTitle, StilEntry_.stilArtist,
						StilEntry_.stilComment)) {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			SearchCriteria<?, ?> criteria = new SearchCriteria(
					singularAttribute);
			result.add(criteria);
		}
		return result;
	}

}