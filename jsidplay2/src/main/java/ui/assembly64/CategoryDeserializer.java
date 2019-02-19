package ui.assembly64;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class CategoryDeserializer extends JsonDeserializer<Category> {

	private List<Category> categories;

	public CategoryDeserializer(Category[] categories) {
		this.categories = Arrays.asList(categories);
	}

	@Override
	public Category deserialize(JsonParser parser, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		final int categoryId = parser.getValueAsInt();
		return categories.stream().filter(category -> category != null && category.getId().intValue() == categoryId)
				.findFirst().orElse(null);
	}

}
