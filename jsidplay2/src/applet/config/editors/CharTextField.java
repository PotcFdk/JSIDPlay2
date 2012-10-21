package applet.config.editors;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class CharTextField extends JTextField {

	public CharTextField() {
		setDocument(new CharTextDocument());
	}

	class CharTextDocument extends PlainDocument {
		@Override
		public void insertString(int offs, String str, AttributeSet a)
				throws BadLocationException {
			if (str == null)
				return;
			String oldString = getText(0, getLength());
			String newString = oldString.substring(0, offs) + str
					+ oldString.substring(offs);
			if (newString.length() == 1) {
				super.insertString(offs, str, a);
			}
		}
	}
}