package applet.config.editors;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class ShortTextField extends JTextField {

	public ShortTextField() {
		setDocument(new IntTextDocument());
	}

	class IntTextDocument extends PlainDocument {
		@Override
		public void insertString(int offs, String str, AttributeSet a)
				throws BadLocationException {
			if (str == null)
				return;
			String oldString = getText(0, getLength());
			String newString = oldString.substring(0, offs) + str
					+ oldString.substring(offs);
			try {
				Short.parseShort(newString + "0");
				super.insertString(offs, str, a);
			} catch (NumberFormatException e) {
			}
		}
	}
}