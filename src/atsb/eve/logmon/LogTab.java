package atsb.eve.logmon;

import java.io.FileNotFoundException;

import atsb.eve.logmon.FileMonitor.NewLineListener;
import atsb.eve.logmon.MetadataScanner.Logfile;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;

public class LogTab extends Tab implements NewLineListener {

	private Logfile logfile;
	private FileMonitor monitor;
	private TextArea text;

	public LogTab(Logfile log) throws FileNotFoundException {
		logfile = log;
		text = new TextArea();
		text.setEditable(false);
		text.setWrapText(true);
		setText(log.channelName);
		setContent(text);
		setClosable(false);

		monitor = new FileMonitor(logfile);
		monitor.setIgnoreExisting(true);
		monitor.addListener(this);
		monitor.start();
	}

	public void replace(Logfile log) throws FileNotFoundException {
		kill();
		logfile = log;
		monitor = new FileMonitor(logfile);
		monitor.addListener(this);
		monitor.start();
	}

	public void kill() {
		if (monitor != null) {
			monitor.removeAllListeners();
			monitor.stop();
			monitor = null;
		}
	}

	@Override
	public void newLine(String line) {
		text.appendText(line + "\n");
	}

}
