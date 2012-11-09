package applet.collection;

import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.Action;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MyDateSpinner extends JSpinner {

	public MyDateSpinner() {
		String pattern = "yyyy";

		// Set current year as default
		Date defaultDate = new Date();
		int year = Calendar.getInstance().get(Calendar.YEAR);
		SimpleDateFormat formatter = new SimpleDateFormat(pattern);
		try {
			defaultDate = formatter.parse(String.valueOf(year));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		setModel(new SpinnerDateModel());
		JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(this, pattern);
		setEditor(dateEditor);

		// Tweak the spinner's formatted text field.
		JFormattedTextField ftf = ((JSpinner.DefaultEditor) dateEditor)
				.getTextField();
		if (ftf != null) {
			ftf.setEditable(false);
			ftf.setColumns(4);
			ftf.setHorizontalAlignment(JTextField.RIGHT);
		}
		setValue(defaultDate);
	}

	public void setAction(final Action action) {
		((JSpinner.DateEditor) getEditor()).getSpinner().addChangeListener(
				new ChangeListener() {

					@Override
					public void stateChanged(ChangeEvent arg0) {
						action.actionPerformed(new ActionEvent(
								arg0.getSource(), 0, null));
					}
				});
	}
}
