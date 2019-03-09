package ui.assembly64;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class CategoryDeserializer extends JsonDeserializer<Category> {

	private List<Category> categories;

	public CategoryDeserializer(List<Category> categories) {
		this.categories = categories;
	}

	@Override
	public Category deserialize(JsonParser parser, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		Integer categoryId = Integer.valueOf(parser.getValueAsInt());
		return categories.stream().filter(category -> category.getId().equals(categoryId)).findFirst().orElse(null);
	}

}
