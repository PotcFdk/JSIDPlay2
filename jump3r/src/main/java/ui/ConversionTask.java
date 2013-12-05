package ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import mp3.Main;

public class ConversionTask extends Service<Void> {

	private int no;
	private String filename;
	private String type;
	private ArrayList<String> cmd;

	public int getNo() {
		return no;
	}

	public void setNo(int no) {
		this.no = no;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public ArrayList<String> getCmd() {
		return cmd;
	}

	public void setCmd(ArrayList<String> cmd) {
		this.cmd = cmd;
	}

	@Override
	protected Task<Void> createTask() {
		return new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, 1);
				updateMessage("Waiting...");

				final Main main = new Main();
				PropertyChangeListener listener = new PropertyChangeListener() {

					@Override
					public void propertyChange(final PropertyChangeEvent evt) {
						if ("progress".equals(evt.getPropertyName())) {
							updateMessage("Running...");
							Integer valueOf = Integer.valueOf(evt.getNewValue()
									.toString());
							updateProgress(valueOf, 100);
							if (valueOf == 100) {
								updateMessage("Done");
								updateProgress(1, 1);
							}
						}
					}
				};
				main.getSupport().addPropertyChangeListener(listener);
				for (String arg : cmd) {
					System.out.print(arg + " ");
				}
				System.out.println();
				main.run(cmd.toArray(new String[cmd.size()]));
				return null;
			}

		};
	}

}
