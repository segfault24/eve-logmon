package dev.pagefault.eve.logmon;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class GameLogTest {

	public static void main(String[] args) {
		String gamelog = "C:\\Users\\" + System.getProperty("user.name") + "\\Documents\\EVE\\logs\\Gamelogs\\20200416_184406.txt";
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(gamelog), StandardCharsets.UTF_8));
			boolean running = true;
			while (running) {
				String line = "";
				if ((line = reader.readLine()) != null) {
					line = line.trim();
					// dont notify on the initial file read if ignore existing is true
					// or if it's an empty line
					if (!line.isEmpty()) {
						System.out.println(line);
					}
				} else {
					break;
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

	public static class GameLogLine {
		//Date timestamp;
		//Category
	}
	
}
