package ui.entities.config;

import static ui.entities.config.Assembly64ColumnType.CATEGORY;
import static ui.entities.config.Assembly64ColumnType.EVENT;
import static ui.entities.config.Assembly64ColumnType.GROUP;
import static ui.entities.config.Assembly64ColumnType.NAME;
import static ui.entities.config.Assembly64ColumnType.RATING;
import static ui.entities.config.Assembly64ColumnType.RELEASED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;

import ui.common.properties.LazyListField;

@Embeddable
@Access(AccessType.PROPERTY)
public class Assembly64Section {

	public static final List<Assembly64Column> DEFAULT_COLUMNS = Arrays.asList(new Assembly64Column(CATEGORY),
			new Assembly64Column(NAME), new Assembly64Column(GROUP), new Assembly64Column(EVENT),
			new Assembly64Column(RELEASED), new Assembly64Column(RATING));

	private LazyListField<Assembly64Column> columns = new LazyListField<>();

	@OneToMany(cascade = CascadeType.ALL)
	public List<Assembly64Column> getColumns() {
		return columns.get(new ArrayList<>(DEFAULT_COLUMNS));
	}

	public void setColumns(List<Assembly64Column> columns) {
		this.columns.set(columns);
	}

}
