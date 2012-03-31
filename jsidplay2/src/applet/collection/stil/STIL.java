package applet.collection.stil;

import java.awt.Container;
import java.awt.Point;

import javax.swing.JDialog;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;

import libsidutils.STIL.Info;
import libsidutils.STIL.STILEntry;
import libsidutils.STIL.TuneEntry;

import org.swixml.SwingEngine;



public class STIL implements TreeSelectionListener {
	protected JTree tree;
	protected JTextPane textpane;

	private static final String STYLE_NORMAL = "STYLE_NORMAL";
	private static final String STYLE_FILENAME = "STYLE_FILENAME";
	private static final String STYLE_COMMENT = "STYLE_COMMENT";
	private static final String STYLE_SUBTUNE = "STYLE_SUBTUNE";
	private static final String STYLE_NAME = "STYLE_NAME";
	private static final String STYLE_AUTHOR = "STYLE_AUTHOR";
	private static final String STYLE_TITLE = "STYLE_TITLE";
	private static final String STYLE_ARTIST = "STYLE_ARTIST";
	private static final String STYLE_INFO = "STYLE_INFO";

	public STIL(STILEntry tuneEntry) {
		try {
			final Container sv = new SwingEngine(this).render(STIL.class
					.getResource("STIL.xml"));

			setDefaultsAndActions(tuneEntry);
			sv.setLocation(new Point(0, 0));
			((JDialog) sv).pack();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
	}
	
	private void setDefaultsAndActions(STILEntry tuneEntry) {
		// Listen for when the selection changes.
		tree.addTreeSelectionListener(this);

		StyledDocument document = textpane.getStyledDocument();

		// Initialize some styles.
		final Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

		final Style regular = document.addStyle("regular", def);
		Style s = document.addStyle(STYLE_NORMAL, regular);

		s = document.addStyle(STYLE_FILENAME, regular);
		StyleConstants.setBold(s, true);

		s = document.addStyle(STYLE_COMMENT, regular);
		StyleConstants.setItalic(s, true);

		s = document.addStyle(STYLE_NAME, regular);
		StyleConstants.setBold(s, true);

		s = document.addStyle(STYLE_TITLE, regular);
		StyleConstants.setBold(s, true);

		s = document.addStyle(STYLE_AUTHOR, regular);
		StyleConstants.setBold(s, true);

		s = document.addStyle(STYLE_ARTIST, regular);
		StyleConstants.setBold(s, true);

		s = document.addStyle(STYLE_SUBTUNE, regular);
		StyleConstants.setBold(s, true);
		StyleConstants.setItalic(s, true);

		s = document.addStyle(STYLE_INFO, regular);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(
				tuneEntry);
		setTextAreaFromTree(tuneEntry);
		((STILTreeModel) tree.getModel()).setRoot(root);
	}

	/**
	 * Show STIL if tree selection changes
	 */
	public void valueChanged(final TreeSelectionEvent event) {
		if (event.getNewLeadSelectionPath() == null) {
			return;
		}
		final Object comp = event.getNewLeadSelectionPath().getLastPathComponent();

		Object obj = ((DefaultMutableTreeNode)comp).getUserObject();
		setTextAreaFromTree(obj);
	}

	private void setTextAreaFromTree(final Object comp) {
		StyledDocument document = textpane.getStyledDocument();
		try {
			document.remove(0, document.getLength());
			writeSTIL(comp);
		}
		catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeSTIL(final Object comp) throws BadLocationException {
		if (comp instanceof STILEntry) {
			final STILEntry entry = (STILEntry) comp;
			writeEntry(entry);
			for (int i = 0; i < entry.infos.size(); i++) {
				writeSTIL(entry.infos.get(i));
			}
			for (int i = 0; i < entry.subtunes.size(); i++) {
				writeSTIL(entry.subtunes.get(i));
			}
		} else if (comp instanceof TuneEntry) {
			final TuneEntry tuneEntry = (TuneEntry) comp;
			writeSubTune(tuneEntry);
			for (int i = 0; i < tuneEntry.infos.size(); i++) {
				writeSTIL(tuneEntry.infos.get(i));
			}
		} else if (comp instanceof Info) {
			final Info info = (Info) comp;
			writeInfo(info);
		}
	}

	private void writeEntry(final STILEntry entry) throws BadLocationException {
		StyledDocument document = textpane.getStyledDocument();
		document.insertString(document.getLength(), "Filename: ", document.getStyle(STYLE_FILENAME));
		document.insertString(document.getLength(), entry.filename, document.getStyle(STYLE_NORMAL));
		document.insertString(document.getLength(), "\n", document.getStyle(STYLE_NORMAL));
		if (entry.globalComment != null) {
			document.insertString(document.getLength(), entry.globalComment.trim(), document.getStyle(STYLE_NORMAL));
			document.insertString(document.getLength(), "\n", document.getStyle(STYLE_NORMAL));
		}
		document.insertString(document.getLength(), "\n", document.getStyle(STYLE_NORMAL));
	}

	private void writeSubTune(final TuneEntry tuneEntry) throws BadLocationException {
		StyledDocument document = textpane.getStyledDocument();
		document.insertString(document.getLength(), "Sub-tune #", document.getStyle(STYLE_SUBTUNE));
		document.insertString(document.getLength(), tuneEntry.tuneNo + ":", document.getStyle(STYLE_NORMAL));
		if (tuneEntry.globalComment != null) {
			document.insertString(document.getLength(), "\n", document.getStyle(STYLE_NORMAL));
			document.insertString(document.getLength(), tuneEntry.globalComment.trim(), document.getStyle(STYLE_COMMENT));
			document.insertString(document.getLength(), "\n", document.getStyle(STYLE_NORMAL));
		}
		document.insertString(document.getLength(), "\n", document.getStyle(STYLE_NORMAL));
	}

	private void writeInfo(final Info info) throws BadLocationException {
		StyledDocument document = textpane.getStyledDocument();
		if (info.comment != null) {
			document.insertString(document.getLength(), info.comment.trim(), document.getStyle(STYLE_NORMAL));
			document.insertString(document.getLength(), "\n", document.getStyle(STYLE_NORMAL));
		}
		if (info.name != null) {
			document.insertString(document.getLength(), "Name: ", document.getStyle(STYLE_NAME));
			document.insertString(document.getLength(), info.name, document.getStyle(STYLE_NORMAL));
			document.insertString(document.getLength(), "\n", document.getStyle(STYLE_NORMAL));
		}
		if (info.author != null) {
			document.insertString(document.getLength(), "Author: ", document.getStyle(STYLE_AUTHOR));
			document.insertString(document.getLength(), info.author, document.getStyle(STYLE_NORMAL));
			document.insertString(document.getLength(), "\n", document.getStyle(STYLE_NORMAL));
		}
		if (info.title != null) {
			document.insertString(document.getLength(), "Title: ", document.getStyle(STYLE_TITLE));
			document.insertString(document.getLength(), info.title, document.getStyle(STYLE_NORMAL));
			document.insertString(document.getLength(), "\n", document.getStyle(STYLE_NORMAL));
		}
		if (info.artist != null) {
			document.insertString(document.getLength(), "Artist: ", document.getStyle(STYLE_ARTIST));
			document.insertString(document.getLength(), info.artist, document.getStyle(STYLE_NORMAL));
			document.insertString(document.getLength(), "\n", document.getStyle(STYLE_NORMAL));
		}
		document.insertString(document.getLength(), "\n", document.getStyle(STYLE_NORMAL));
	}
}
