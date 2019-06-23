package atsb.eve.logmon;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import atsb.eve.logmon.DirectoryMonitor.NewFileListener;
import atsb.eve.logmon.MetadataScanner.Logfile;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class LogMon extends Application implements NewFileListener {

	private TabPane mainPane;
	private Map<String, Tab> charTabs;
	private Map<String, LogTab> logTabs;
	private DirectoryMonitor dmon;

	public LogMon() {
		mainPane = new TabPane();
		charTabs = new TreeMap<String, Tab>();
		logTabs = new TreeMap<String, LogTab>();

		String dir = "C:\\Users\\" + System.getProperty("user.name") + "\\Documents\\EVE\\logs\\Chatlogs";
		monInit(dir);
		try {
			dmon = new DirectoryMonitor(dir);
			dmon.addListener(this);
			dmon.start();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void start(Stage stage) throws Exception {
		stage.setTitle("Eve Logmon");
		Scene scene = new Scene(mainPane, 600, 400);
		stage.setMinHeight(200);
		stage.setMinWidth(300);
		stage.setScene(scene);
		stage.show();
	}

	@Override
	public void stop() {
		for (LogTab l : logTabs.values()) {
			if (l != null) {
				l.kill();
			}
		}
		if (dmon != null) {
			dmon.stop();
		}
	}

	private void monInit(String dirname) {
		File dir = new File(dirname);
		if (!dir.exists() || !dir.isDirectory()) {
			return;
		}

		// build a list of files that were started within the last 24hrs
		// since we need to open each file to see who the listener is, this step
		// significantly reduces the initial load time on large log directories
		List<Logfile> recentLogs = new ArrayList<Logfile>();
		for (String filename : dir.list()) {
			Logfile log;
			try {
				log = MetadataScanner.quickScan(new File(dirname + File.separator + filename));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				continue;
			}
			if (log.started.isAfter(LocalDateTime.now().minusDays(1))) {
				recentLogs.add(log);
			}
		}

		// find the latest log, per channel, per listener
		Map<String, Logfile> initial = new TreeMap<String, Logfile>();
		for (Logfile l : recentLogs) {
			MetadataScanner.deepScan(l);
			if (initial.containsKey(l.key())) {
				if (l.started.isAfter(initial.get(l.key()).started)) {
					initial.replace(l.key(), l);
				}
			} else {
				initial.put(l.key(), l);
			}
		}

		// finally build the tabs (which start the listeners)
		for (Logfile l : initial.values()) {
			try {
				buildNewLogTab(l);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Assumes there is not already a tab for this character/channel combination
	 * 
	 * @param logfile
	 * @throws FileNotFoundException
	 */
	private synchronized void buildNewLogTab(Logfile logfile) throws FileNotFoundException {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				LogTab logtab;
				try {
					logtab = new LogTab(logfile);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					return;
				}

				if (charTabs.containsKey(logfile.channelListener)) {
					// add to existing char tab
					Tab charTab = charTabs.get(logfile.channelListener);
					TabPane tabpane = (TabPane) charTab.getContent();
					tabpane.getTabs().add(logtab);
				} else {
					// create new character tab
					Tab charTab = new Tab(logfile.channelListener);
					TabPane tabpane = new TabPane();
					tabpane.getTabs().add(logtab);
					charTab.setContent(tabpane);
					charTab.setClosable(false);
					mainPane.getTabs().add(charTab);
					charTabs.put(logfile.channelListener, charTab);
				}
				logTabs.put(logfile.key(), logtab);
			}
		});
	}

	@Override
	public void newFile(File file) {
		try {
			Logfile log;
			try {
				log = MetadataScanner.quickScan(file);
				MetadataScanner.deepScan(log);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				return;
			}
			System.out.println("Found new file [" + log.key() + "] : " + file.getName());

			if (logTabs.containsKey(log.key())) {
				logTabs.get(log.key()).replace(log);
			} else {
				buildNewLogTab(log);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/*if (line.toLowerCase().contains("tackled")) {
		try {
			File f = new File("structure.wav").getAbsoluteFile();
			AudioInputStream audioIn = AudioSystem.getAudioInputStream(f);
			Clip clip = AudioSystem.getClip();
			clip.open(audioIn);
			clip.start();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
	}*/

	public static void main(String[] args) {
		launch(args);
	}

}
