package atsb.eve.logmon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import atsb.eve.logmon.FileMonitor.NewLineListener;
import atsb.eve.logmon.MetadataScanner.Logfile;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;

public class LogTab extends Tab implements NewLineListener {

	private Logfile logfile;
	private FileMonitor monitor;
	private TextArea text;
	private int maxLines = 100;
	private ObservableList<Filter> filters;

	public LogTab(Logfile log) throws FileNotFoundException {
		filters = FXCollections.observableArrayList();
		logfile = log;
		text = new TextArea() {
			@Override
			public void replaceText(int start, int end, String text) {
				super.replaceText(start, end, text);
				while (getText().split("\n", -1).length > maxLines) {
					int fle = getText().indexOf("\n");
					super.replaceText(0, fle + 1, "");
				}
				positionCaret(getText().length());
			}
		};
		text.appendText("");
		text.setEditable(false);
		text.setWrapText(true);

		setText(log.channelName);
		setContent(text);
		setClosable(false);

		monitor = new FileMonitor(logfile);
		//monitor.setIgnoreExisting(true);
		monitor.addListener(this);
		monitor.start();
	}

	public void replace(Logfile log) throws FileNotFoundException {
		kill();
		logfile = log;
		monitor = new FileMonitor(logfile);
		//monitor.setIgnoreExisting(true);
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

	public void setFilters(ObservableList<Filter> filters) {
		this.filters = filters;
	}

	@Override
	public void newLine(boolean firstRead, String line) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				text.appendText(line + "\n");
			}
		});
		if (!firstRead) {
			for (Filter filter : filters) {
				if (Pattern.compile(filter.getExpression()).matcher(line).find()) {
					try {
						File f = new File("blip.wav").getAbsoluteFile();
						AudioInputStream audioIn = AudioSystem.getAudioInputStream(f);
						Clip clip = AudioSystem.getClip();
						clip.open(audioIn);
						clip.start();
						if (!getStyleClass().contains("alerted"))
							getStyleClass().add("alerted");
					} catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
						e.printStackTrace();
					}
					break;
				}
			}
		}
	}

}
