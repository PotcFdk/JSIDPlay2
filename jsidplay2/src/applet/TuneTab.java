package applet;

import javax.swing.JPanel;

import libsidplay.Player;
import libsidplay.sidtune.SidTune;
import applet.events.UIEventFactory;
import applet.events.UIEventListener;

public abstract class TuneTab extends JPanel implements UIEventListener {
	
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
