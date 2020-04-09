package atsb.eve.logmon;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import atsb.eve.logmon.MetadataScanner.Logfile;

/**
 * Monitors a chatlog file for new lines
 * 
 * @author austin
 */
public class FileMonitor {

	private static final long SLEEP_MILLIS = 500;

	private Logfile logfile;
	private List<NewLineListener> listeners;
	private Thread t;
	private boolean paused;

	public FileMonitor(Logfile log) throws FileNotFoundException {
		if (log.file == null || !log.file.isFile() || !log.file.exists()) {
			throw new FileNotFoundException();
		}
		logfile = log;
		listeners = new ArrayList<NewLineListener>();
		paused = false;
	}

	public Logfile getLogfile() {
		return logfile;
	}

	public synchronized void start() {
		if (t != null) {
			return;
		}
		t = new Thread(new Runnable() {
			@Override
			public void run() {
				boolean firstRead = true;
				BufferedReader reader = null;
				try {
					reader = new BufferedReader(
							new InputStreamReader(new FileInputStream(logfile.file), StandardCharsets.UTF_16));
					boolean running = true;
					while (running) {
						String line = "";
						if (!paused && (line = reader.readLine()) != null) {
							line = line.trim();
							// dont notify on the initial file read if ignore existing is true
							// or if it's an empty line
							if (!line.isEmpty()) {
								notifyListeners(firstRead, line);
							}
						} else {
							firstRead = false;
							try {
								Thread.sleep(SLEEP_MILLIS);
							} catch (InterruptedException ex) {
								running = false;
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
		t.start();
		System.out.println("Started watching [" + logfile.key() + "] : " + logfile.file.getName());
	}

	public synchronized void stop() {
		if (t == null) {
			return;
		}
		try {
			t.interrupt();
			t.join(2000);
		} catch (InterruptedException e) {
		}
		t = null;
		System.out.println("Stopped watching [" + logfile.key() + "] : " + logfile.file.getName());
	}

	public synchronized void pause() {
		paused = true;
	}

	public synchronized void resume() {
		paused = false;
	}

	private void notifyListeners(boolean firstRead, String line) {
		synchronized (listeners) {
			for (NewLineListener l : listeners) {
				l.newLine(firstRead, line);
			}
		}
	}

	public void addListener(NewLineListener l) {
		synchronized (listeners) {
			if (l != null && !listeners.contains(l)) {
				listeners.add(l);
			}
		}
	}

	public void removeListener(NewLineListener l) {
		synchronized (listeners) {
			if (l != null && listeners.contains(l)) {
				listeners.remove(l);
			}
		}
	}

	public void removeAllListeners() {
		synchronized (listeners) {
			listeners.clear();
		}
	}

	public interface NewLineListener {
		public void newLine(boolean firstRead, String line);
	}

}
