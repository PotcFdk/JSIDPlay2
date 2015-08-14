package ui.musiccollection;

import java.util.ResourceBundle;

import javafx.util.StringConverter;

import javax.persistence.metamodel.SingularAttribute;

public final class SearchCriteriaToString extends
		StringConverter<SearchCriteria<?, ?>> {

	private final ResourceBundle bundle;

	public SearchCriteriaToString(ResourceBundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public SearchCriteria<?, ?> fromString(String string) {
		throw new RuntimeException("This function is not supported!");
	}

	@Override
	public String toString(SearchCriteria<?, ?> object) {
		SingularAttribute<?, ?> attribute = object.getAttribute();
		return bundle.getString(attribute.getDeclaringType().getJavaType()
				.getSimpleName()
				+ "." + attribute.getName());
	}
}