package ui.entities.config;

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

	public static final List<Assembly64Column> DEFAULT_COLUMNS = Arrays.asList(
			new Assembly64Column(Assembly64ColumnType.CATEGORY), new Assembly64Column(Assembly64ColumnType.NAME),
			new Assembly64Column(Assembly64ColumnType.GROUP), new Assembly64Column(Assembly64ColumnType.EVENT),
			new Assembly64Column(Assembly64ColumnType.RELEASED), new Assembly64Column(Assembly64ColumnType.RATING));

	private LazyListField<Assembly64Column> columns = new LazyListField<>();

	@OneToMany(cascade = CascadeType.ALL)
	public List<Assembly64Column> getColumns() {
		return columns.get(new ArrayList<>(DEFAULT_COLUMNS));
	}

	public void setColumns(List<Assembly64Column> columns) {
		this.columns.set(columns);
	}

}
