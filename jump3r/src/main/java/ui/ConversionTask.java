package ui;

import java.io.File;
import java.util.ArrayList;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import mp3.Main;

public class ConversionTask extends Service<Void> {

	private int no;
	private File file;
	private String type;
	private ArrayList<String> cmd;

	public int getNo() {
		return no;
	}

	public void setNo(int no) {
		this.no = no;
	}

	public File getFile() {
		return file;
	}

	public String getType() {
		return type;
	}

	public void setFile(File file) {
		this.file = file;
		this.type = file.getName().substring(
				file.getName().lastIndexOf('.') + 1);
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
				main.getSupport().addPropertyChangeListener(
						(evt) -> {
							if ("progress".equals(evt.getPropertyName())) {
								updateMessage("Running...");
								updateProgress(Integer.valueOf(evt
										.getNewValue().toString()), 100);
							}
						});
				for (String arg : cmd) {
					System.out.print(arg + " ");
				}
				System.out.println();
				if (0 != main.run(cmd.toArray(new String[cmd.size()]))) {
					updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, 1);
					updateMessage("Waiting...");
				} else {
					updateMessage("Done");
					updateProgress(1, 1);
				}
				return null;
			}

		};
	}

}
