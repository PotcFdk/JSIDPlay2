package applet.favorites;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import applet.JSIDPlay2;
import applet.events.UIEventFactory;
import applet.events.favorites.IChangeFavoritesTab;
import applet.ui.JNiceButton;

/**
 * Component to be used as tabComponent; Contains a JLabel to show the text and
 * a JButton to close the tab it belongs to
 */
public class ButtonTabComponent extends JPanel {
	protected UIEventFactory uiEvents = UIEventFactory.getInstance();
	public ImageIcon m_icon;
	public JButton fPlayButton = new JNiceButton(new ImageIcon(
			JSIDPlay2.class.getResource("icons/play.png")), true);

	public ButtonTabComponent(final JTabbedPane pane, String title,
			String iconName, String toolTip, ActionListener listener) {
		// unset default FlowLayout' gaps
		super(new FlowLayout(FlowLayout.LEFT, 0, 0));
		if (pane == null) {
			throw new NullPointerException("TabbedPane is null");
		}
		this.m_icon = new ImageIcon(JSIDPlay2.class.getResource(iconName));
		setOpaque(false);

		// make JLabel read titles from JTabbedPane
		final JTextField label = new JTextField(title);
		label.setOpaque(false);
		label.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 1) {
					int index = pane
							.indexOfTabComponent(ButtonTabComponent.this);
					pane.setSelectedIndex(index);
				}
			}

		});
		pane.addPropertyChangeListener("indexForTitle",
				new PropertyChangeListener() {

					public void propertyChange(PropertyChangeEvent evt) {
						int index = pane
								.indexOfTabComponent(ButtonTabComponent.this);
						if ((Integer) evt.getNewValue() == index) {
							label.setText(pane.getTitleAt((Integer) evt
									.getNewValue()));
							pane.repaint();
						}
					}

				});
		label.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				int index = pane.indexOfTabComponent(ButtonTabComponent.this);
				if (label.getText().equals(pane.getTitleAt(index))) {
					return;
				}
				uiEvents.fireEvent(IChangeFavoritesTab.class,
						new IChangeFavoritesTab() {
							public int getIndex() {
								return pane
										.indexOfTabComponent(ButtonTabComponent.this);
							}

							public String getTitle() {
								return label.getText();
							}

							public String getFileName() {
								Component comp = pane.getComponentAt(pane
										.indexOfTabComponent(ButtonTabComponent.this));
								if (comp instanceof IFavorites) {
									IFavorites fav = (IFavorites) comp;
									return fav.getFileName();
								}

								return null;
							}

							public boolean isSelected() {
								return false;
							}
						});
			}

		});
		fPlayButton.setVisible(false);
		add(fPlayButton);
		add(label);
		// add more space between the label and the button
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
		// tab button
		JButton button = new TabButton(this, listener, toolTip);
		add(button);
		// add more space to the top of the component
		setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
	}

	public class TabButton extends JNiceButton {
		private final JComponent fComp;

		public TabButton(JComponent comp, ActionListener listener,
				String toolTip) {
			this.fComp = comp;
			setIcon(m_icon);
			setToolTipText(toolTip);
			// we use the same listener for all buttons
			// Close the proper tab by clicking the button
			addActionListener(listener);
		}

		// we don't want to update UI for this button
		@Override
		public void updateUI() {
		}

		public JComponent getComp() {
			return fComp;
		}
	}

}
