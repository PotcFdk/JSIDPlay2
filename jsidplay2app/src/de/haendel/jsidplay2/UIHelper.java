package de.haendel.jsidplay2;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;

public class UIHelper {

	private SharedPreferences preferences;

	public UIHelper(SharedPreferences preferences) {
		this.preferences = preferences;
	}

	public final void setupEditText(final EditText editText,
			final String parName, final String defaultValue) {
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				String newValue = editText.getText().toString();
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(parName, newValue);
				editor.commit();
				editTextUpdated(parName, newValue);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
		});
		editText.setText(preferences.getString(parName, defaultValue));
	}

	public final void setupCheckBox(final CheckBox checkBox,
			final String parName, final String defaultValue) {
		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				boolean newValue = checkBox.isChecked();
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(parName, Boolean.toString(newValue));
				editor.commit();
				checkBoxUpdated(parName, newValue);
			}

		});
		checkBox.setChecked(Boolean.valueOf(preferences.getString(parName,
				defaultValue)));
	}

	public final void setupSpinner(final Context context,
			final Spinner spinner, final String[] items, final String parName,
			final String defaultValue) {
		spinner.setAdapter(new ArrayAdapter<String>(context,
				android.R.layout.simple_spinner_item, items));
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				String newValue = spinner.getSelectedItem().toString();
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(parName, newValue);
				editor.commit();
				spinnerUpdated(parName, newValue);
			}

			public void onNothingSelected(android.widget.AdapterView<?> parent) {
			}
		});
		String value = preferences.getString(parName, defaultValue);
		for (int i = 0; i < items.length; i++) {
			if (items[i].equals(value)) {
				spinner.setSelection(i);
				spinnerUpdated(parName, value);
				break;
			}
		}
	}

	protected void editTextUpdated(String parName, String newValue) {
	}

	protected void checkBoxUpdated(String parName, boolean newValue) {
	}

	protected void spinnerUpdated(String parName, String newValue) {
	}

}
