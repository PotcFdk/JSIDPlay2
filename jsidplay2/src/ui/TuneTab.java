package ui;

import javax.swing.JPanel;

import ui.events.UIEventFactory;
import ui.events.UIEventListener;

import libsidplay.Player;
import libsidplay.sidtune.SidTune;

public abstract class TuneTab extends JPanel implements UIEventListener {
	
	private static final long serialVersionUID = -7593368914170407583L;

	private UIEventFactory uiEvents = UIEventFactory.getInstance();

	protected TuneTab() {
		uiEvents.addListener(this);
	}
	
	/**
	 * A tune is loaded
	 * @param m_engine
	 * @param m_tune
	 */
	public abstract void setTune(Player m_engine, SidTune m_tune);

	public UIEventFactory getUiEvents() {
		return uiEvents;
	}
}
