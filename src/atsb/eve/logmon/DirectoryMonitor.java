package atsb.eve.logmon;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Monitors a directory for the appearance of new files
 * 
 * @author austin
 */
public class DirectoryMonitor {

	private static final long SLEEP_MILLIS = 5000;

	private File directory;
	private List<NewFileListener> listeners;
	private Thread t;

	public DirectoryMonitor(String dir) throws FileNotFoundException {
		directory = new File(dir);
		if (!directory.isDirectory()) {
			throw new FileNotFoundException();
		}
		listeners = new ArrayList<NewFileListener>();
	}

	public synchronized void start() {
		if (t != null) {
			return;
		}
		t = new Thread(new Runnable() {
			@Override
			public void run() {
				boolean firstRead = true;
				List<String> monitored = new ArrayList<String>();
				while (true) {
					for (String filename : directory.list()) {
						if (monitored.contains(filename)) {
							continue;
						}
						File file = new File(directory.getAbsolutePath() + File.separator + filename);
						if (file.exists() && file.isFile()) {
							monitored.add(filename);
							if (firstRead) {
								// dont notify on the initial directory read
							} else {
								notifyListeners(file);
							}
						}
					}
					firstRead = false;
					try {
						Thread.sleep(SLEEP_MILLIS);
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		});
		t.start();
		System.out.println("Started watching : " + directory.getAbsolutePath());
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
		System.out.println("Stopped watching : " + directory.getAbsolutePath());
	}

	private void notifyListeners(File file) {
		synchronized (listeners) {
			for (NewFileListener l : listeners) {
				l.newFile(file);
			}
		}
	}

	public void addListener(NewFileListener l) {
		synchronized (listeners) {
			if (l != null && !listeners.contains(l)) {
				listeners.add(l);
			}
		}
	}

	public void removeListener(NewFileListener l) {
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

	public interface NewFileListener {
		public void newFile(File file);
	}

}
