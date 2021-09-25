package dev.pagefault.eve.logmon;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import dev.pagefault.eve.logmon.DirectoryMonitor.NewFileListener;
import dev.pagefault.eve.logmon.MetadataScanner.ChatLogFile;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class LogMon extends Application implements NewFileListener {

	private TabPane mainPane;
	private Map<String, Tab> charTabs;
	private Map<String, LogTab> logTabs;
	private DirectoryMonitor dmon;
	private ObservableList<Filter> filters;

	public LogMon() {
		mainPane = new TabPane();
		charTabs = new TreeMap<String, Tab>();
		logTabs = new TreeMap<String, LogTab>();
		filters = FXCollections.observableArrayList();
		for (String f : ConfigurationData.getInstance().getListProperty("GlobalFilters")) {
			filters.add(new Filter(f));
		}
	}

	@Override
	public void start(Stage stage) throws Exception {
		stage.setTitle("Eve Logmon");

		MenuBar menuBar = new MenuBar();
		Menu fileMenu = new Menu("File");
		MenuItem exitItem = new MenuItem("Exit");
		exitItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				ConfigurationData.getInstance().save();
				Platform.exit();
			}
		});
		fileMenu.getItems().addAll(exitItem);

		Menu filterMenu = new Menu("Filters");
		MenuItem globalFiltersItem = new MenuItem("Global");
		globalFiltersItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				FilterTableStage fts = new FilterTableStage(filters);
				fts.show();
			}
		});
		MenuItem characterFiltersItem = new MenuItem("Character");
		characterFiltersItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
			}
		});
		MenuItem channelFiltersItem = new MenuItem("Channel");
		channelFiltersItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
			}
		});
		filterMenu.getItems().addAll(globalFiltersItem, characterFiltersItem, channelFiltersItem);

		menuBar.getMenus().addAll(fileMenu, filterMenu);

		BorderPane bp = new BorderPane();
		bp.setTop(menuBar);
		bp.setCenter(mainPane);
		Scene scene = new Scene(bp, 600, 400);
		stage.setMinHeight(200);
		stage.setMinWidth(300);
		scene.getStylesheets().add(new File("stylesheet.css").toURI().toURL().toString());
		stage.setScene(scene);
		stage.show();

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				startChatLogMonitor();
			}
		});
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
		ConfigurationData.getInstance().save();
	}

	private void startChatLogMonitor() {
		String chatDirname = "C:\\Users\\" + System.getProperty("user.name") + "\\Documents\\EVE\\logs\\Chatlogs";
		File dir = new File(chatDirname);
		if (!dir.exists() || !dir.isDirectory()) {
			return;
		}

		// build a list of files that were started within the last 24hrs
		// since we need to open each file to see who the listener is, this step
		// significantly reduces the initial load time on large log directories
		List<ChatLogFile> recentLogs = new ArrayList<ChatLogFile>();
		for (String filename : dir.list()) {
			ChatLogFile log;
			try {
				log = MetadataScanner.quickScanChatLog(new File(chatDirname + File.separator + filename));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				continue;
			}
			if (log.started.isAfter(LocalDateTime.now().minusDays(1))) {
				recentLogs.add(log);
			}
		}

		// find the latest log, per channel, per listener
		Map<String, ChatLogFile> initial = new TreeMap<String, ChatLogFile>();
		for (ChatLogFile l : recentLogs) {
			MetadataScanner.deepScanChatLog(l);
			if (initial.containsKey(l.key())) {
				if (l.started.isAfter(initial.get(l.key()).started)) {
					initial.replace(l.key(), l);
				}
			} else {
				initial.put(l.key(), l);
			}
		}

		// finally build the tabs (which start the listeners)
		for (ChatLogFile l : initial.values()) {
			try {
				buildNewLogTab(l);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		// start to monitor for new files
		try {
			dmon = new DirectoryMonitor(chatDirname);
			dmon.addListener(this);
			dmon.start();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void startGameLogMonitor() {
		String gameDirname = "C:\\Users\\" + System.getProperty("user.name") + "\\Documents\\EVE\\logs\\Gamelogs";
	}

	/**
	 * Assumes there is not already a tab for this character/channel combination
	 * 
	 * @param logfile
	 * @throws FileNotFoundException
	 */
	private synchronized void buildNewLogTab(final ChatLogFile logfile) throws FileNotFoundException {
		ArrayList<String> ignores = ConfigurationData.getInstance().getListProperty("GlobalChannelIgnores");
		for (String i : ignores) {
			if (Pattern.compile(i).matcher(logfile.channelName).find()) {
				return;
			}
		}

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				LogTab logtab;
				try {
					logtab = new LogTab(logfile);
					logtab.setFilters(filters);
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
					final TabPane tabpane = new TabPane();
					tabpane.getTabs().add(logtab);
					charTab.setContent(tabpane);
					charTab.setClosable(false);
					mainPane.getTabs().add(charTab);
					charTabs.put(logfile.channelListener, charTab);

					// clear alerted style on tab select
					tabpane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
						@Override
						public void changed(ObservableValue<? extends Tab> arg0, Tab oldTab, Tab newTab) {
							newTab.getStyleClass().remove("alerted");
							tabpane.requestLayout();
						}
					});
				}
				logTabs.put(logfile.key(), logtab);
			}
		});
	}

	@Override
	public void newFile(final File file) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try {
					ChatLogFile log;
					try {
						log = MetadataScanner.quickScanChatLog(file);
						MetadataScanner.deepScanChatLog(log);
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
		});
	}

	public static void main(String[] args) {
		launch(args);
	}

}
