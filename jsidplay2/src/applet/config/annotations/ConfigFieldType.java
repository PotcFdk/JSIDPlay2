package applet.config.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigFieldType {
	/**
	 * Is the configurable field a filename?
	 * 
	 * @return configurable field is a filename
	 */
	Class<?> uiClass();

	/**
	 * Get files and/or folders mode
	 * 
	 * @return e.g. JFileChooser.DIRECTORIES_ONLY
	 */
	int filter();

}
