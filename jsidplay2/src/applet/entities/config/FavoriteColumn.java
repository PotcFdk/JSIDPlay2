package applet.entities.config;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlTransient;

import applet.config.annotations.ConfigClass;
import applet.config.annotations.ConfigDescription;
import applet.config.annotations.ConfigTransient;

@Entity
@ConfigClass(bundleKey = "FAVORITE_COLUMN")
public class FavoriteColumn {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@ConfigTransient
	private Integer id;

	@XmlTransient
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ConfigDescription(descriptionKey = "FAVORITES_COLUMNS_COLUMN_PROPERTY_DESC", toolTipKey = "FAVORITES_COLUMNS_COLUMN_PROPERTY_TOOLTIP")
	private String columnProperty;

	public String getColumnProperty() {
		return columnProperty;
	}

	public void setColumnProperty(String columnProperty) {
		this.columnProperty = columnProperty;
	}

}
