package applet.printer;

import libsidplay.Player;
import libsidplay.components.printer.mps803.MPS803;
import libsidplay.sidtune.SidTune;

import org.swixml.SwingEngine;

import applet.TuneTab;
import applet.events.UIEvent;

public class PrinterView extends TuneTab {

	private MPS803 printer;

	protected GraphicsPaper paper;

	public PrinterView(final Player player) {
		printer = player.getPrinter();
		createContents();
	}

	private void createContents() {
		try {
			SwingEngine swix = new SwingEngine(this);
			swix.getTaglib().registerTag("Paper", GraphicsPaper.class);
			swix.insert(PrinterView.class.getResource("Printer.xml"), this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void notify(UIEvent evt) {
	}

	@Override
	public void setTune(Player m_engine, SidTune m_tune) {
		printer.setPaper(paper);
	}

}
