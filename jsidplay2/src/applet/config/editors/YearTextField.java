package applet.config.editors;

import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.Action;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class YearTextField extends JSpinner {
	private static final String PATTERN = "yyyy";

	public YearTextField() {
		// Set current year as default
		Date defaultDate = new Date();
		int year = Calendar.getInstance().get(Calendar.YEAR);
		SimpleDateFormat formatter = new SimpleDateFormat(PATTERN);
		try {
			defaultDate = formatter.parse(String.valueOf(year));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		setModel(new SpinnerDateModel());
		JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(this, PATTERN);
		setEditor(dateEditor);

		JFormattedTextField ftf = ((JSpinner.DefaultEditor) dateEditor)
				.getTextField();
		if (ftf != null) {
			ftf.setEditable(false);
			ftf.setColumns(PATTERN.length());
		}
		setValue(defaultDate);
	}

	public void setAction(final Action action) {
		((JSpinner.DateEditor) getEditor()).getSpinner().addChangeListener(
				new ChangeListener() {

					@Override
					public void stateChanged(ChangeEvent event) {
						action.actionPerformed(new ActionEvent(event
								.getSource(), 0, null));
					}
				});
	}
}
