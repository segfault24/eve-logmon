package atsb.eve.logmon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Functions to extract metadata from chatlog files
 * 
 * @author austin
 */
public class MetadataScanner {

	public static Logfile scanDir(File f) {
		return null;
	}

	/**
	 * Extracts channel name and session start time from the chatlog's filename
	 * 
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 */
	public static Logfile quickScan(File file) throws FileNotFoundException {
		if (file == null || !file.exists() || !file.isFile()) {
			throw new FileNotFoundException();
		}
		//System.out.println("Quick scanning metadata : " + file.getName());

		String s = file.getName();
		LocalDateTime date = LocalDateTime.now();
		String channel = "unknown";
		try {
			String dateStr = s.substring(s.lastIndexOf("_", s.lastIndexOf("_") - 1) + 1, s.lastIndexOf(".txt")).trim();
			channel = s.substring(0, s.indexOf(dateStr) - 1);
			DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyMMdd_HHmmss");
			date = LocalDateTime.parse(dateStr, fmt);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Logfile log = new Logfile();
		log.file = file;
		log.channelName = channel;
		log.started = date;
		return log;
	}

	/**
	 * Reads the chatlog's contents for additional metadata
	 * 
	 * @param logfile
	 */
	public static void deepScan(Logfile logfile) {
		if (logfile.file == null || !logfile.file.exists() || !logfile.file.isFile()) {
			return;
		}
		//System.out.println("Deep scanning metadata : " + logfile.file.getName());

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(logfile.file), StandardCharsets.UTF_16));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		}
		String line = "";
		try {
			while ((line = reader.readLine()) != null) {
				String[] p = line.split(":");
				if (p.length > 1) {
					if (p[0].trim().equalsIgnoreCase("listener")) {
						logfile.channelListener = p[1].trim();
					} else if (p[0].trim().equalsIgnoreCase("channel name")) {
						logfile.channelName = p[1].trim();
					} else if (p[0].trim().equalsIgnoreCase("channel id")) {
						logfile.channelId = p[1].trim();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (logfile.channelName.toLowerCase().startsWith("fleet")) {
			logfile.channelName = "Fleet";
		}
	}

	public static class Logfile {
		public String channelId = "unknown";
		public String channelName = "unknown";
		public String channelListener = "unknown";
		public LocalDateTime started;
		public File file;

		public String key() {
			return channelName + ":" + channelListener;
		}
	}

}
