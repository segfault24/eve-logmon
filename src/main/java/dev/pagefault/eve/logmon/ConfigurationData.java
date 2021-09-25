package dev.pagefault.eve.logmon;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

public class ConfigurationData extends Properties {

	private static final long serialVersionUID = 9173109232467803316L;
	private static ConfigurationData config;

	public static ConfigurationData getInstance() {
		if (config == null) {
			config = new ConfigurationData();
			config.load();
		}
		return config;
	}

	private void load() {
		try {
			load(new FileInputStream("cfg/config.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void save() {
		try {
			store(new FileOutputStream("cfg/config.properties"), null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<String> getListProperty(String key) {
		String r = getProperty(key);
		ArrayList<String> vs = new ArrayList<String>();
		if (r != null) {
			for (String v : r.split(",")) {
				v =v.trim();
				if (!v.isEmpty()) {
					vs.add(v);
				}
			}
		}
		return vs;
	}

	public void setListProperty(String key, ArrayList<String> values) {
		setProperty(key, String.join(",", values));
	}

}
